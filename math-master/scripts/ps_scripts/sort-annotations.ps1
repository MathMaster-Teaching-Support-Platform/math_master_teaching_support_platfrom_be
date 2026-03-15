# sort-annotations.ps1
# Standardizes Lombok annotation ordering in entity Java files.
#
# Target ordering:
#   @Builder > @AllArgsConstructor > @NoArgsConstructor > @Data >
#   @Getter  > @Setter             > @EqualsAndHashCode > @FieldDefaults >
#   @Entity  > @AttributeOverride(s) > @Table
#
# Multi-line annotations (e.g. @Table with indexes) are handled as a single unit.
# JavaDoc comments and blank lines are preserved in their original positions.
#
# Usage:
#   .\sort-annotations.ps1           # fix entity files in-place
#   .\sort-annotations.ps1 -DryRun   # preview only, no writes

[CmdletBinding()]
param(
    [switch]$DryRun,
    [string]$EntityDir = ""
)

# ─── Annotation priority map (lower = appears earlier in block) ───────────────
$PRIORITY = @{
    'Builder'            = 1
    'AllArgsConstructor' = 2
    'NoArgsConstructor'  = 3
    'Data'               = 4
    'Getter'             = 5
    'Setter'             = 6
    'EqualsAndHashCode'  = 7
    'FieldDefaults'      = 8
    'Entity'             = 9
    'AttributeOverrides' = 10
    'AttributeOverride'  = 10
    'Inheritance'        = 11
    'Table'              = 12
}

function Get-Priority([string]$firstLine) {
    if ($firstLine -match '@(\w+)') {
        $name = $Matches[1]
        if ($PRIORITY.ContainsKey($name)) { return $PRIORITY[$name] }
    }
    return 99  # unknown annotations sort to the end
}

# Split a flat list of annotation lines into logical units.
# Each unit = one annotation, possibly spanning multiple lines (e.g. @Table(...)).
function Split-AnnotationUnits([string[]]$lines) {
    $units   = [System.Collections.Generic.List[object]]::new()
    $current = [System.Collections.Generic.List[string]]::new()
    $depth   = 0

    foreach ($line in $lines) {
        $t = $line.TrimStart()
        # Start a new unit when we see @ at paren depth 0 (and we already have something)
        if ($t -match '^@\w' -and $depth -eq 0 -and $current.Count -gt 0) {
            $units.Add($current.ToArray())
            $current = [System.Collections.Generic.List[string]]::new()
        }
        $current.Add($line)
        foreach ($ch in $line.ToCharArray()) {
            if     ($ch -eq '(')                  { $depth++ }
            elseif ($ch -eq ')' -and $depth -gt 0) { $depth-- }
        }
    }
    if ($current.Count -gt 0) { $units.Add($current.ToArray()) }
    return $units
}

# ─── Resolve entity directory ─────────────────────────────────────────────────
$targetDir = if ($EntityDir) {
    $EntityDir
} else {
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    Join-Path (Split-Path (Split-Path $scriptDir)) "src\main\java\com\fptu\math_master\entity"
}

if (-not (Test-Path $targetDir)) {
    Write-Host "  Entity directory not found: $targetDir" -ForegroundColor Red
    return
}

$javaFiles    = Get-ChildItem -Path $targetDir -Filter "*.java"
$changedCount = 0
$enc          = [System.Text.UTF8Encoding]::new($false)   # UTF-8, no BOM

foreach ($file in $javaFiles) {
    $lines = [System.IO.File]::ReadAllLines($file.FullName, $enc)

    # ── Find class declaration ─────────────────────────────────────────────────
    $classIdx = -1
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^\s*(public|protected)?\s*(abstract\s+)?class\s+\w+') {
            $classIdx = $i; break
        }
    }
    if ($classIdx -lt 0) { continue }

    # ── Find end of header (last import / package line) ────────────────────────
    # Stop early if we encounter a JavaDoc comment or an annotation.
    $headerEndIdx = -1
    for ($i = 0; $i -lt $classIdx; $i++) {
        $t = $lines[$i].TrimStart()
        if ($t.StartsWith('import ') -or $t.StartsWith('package ')) {
            $headerEndIdx = $i
        } elseif ($t.StartsWith('/**') -or ($t -match '^@\w')) {
            break
        }
    }

    # ── Locate annotation block using paren-depth tracking ────────────────────
    # - Lines between `/**` comment and `public class` are NOT part of the block.
    # - Multi-line annotations (depth > 0) are collected as one unit.
    $annotStart = -1; $annotEnd = -1
    $dep = 0; $inBlock = $false

    for ($i = $headerEndIdx + 1; $i -lt $classIdx; $i++) {
        $t = $lines[$i].TrimStart()

        if ($t -match '^@\w' -and $dep -eq 0) {
            if (-not $inBlock) { $annotStart = $i; $inBlock = $true }
        }

        if ($inBlock) {
            # Include this line if we are inside a multi-line annotation (depth > 0)
            # or if this line itself starts a new annotation.
            $include = ($dep -gt 0) -or ($t -match '^@\w')

            foreach ($ch in $lines[$i].ToCharArray()) {
                if     ($ch -eq '(')                  { $dep++ }
                elseif ($ch -eq ')' -and $dep -gt 0)  { $dep-- }
            }

            if ($include) { $annotEnd = $i }
            # If depth returned to 0 and next line won't be @, the block may end here —
            # handled naturally: the next iteration's $include will be false unless @ found.
        }
    }

    if ($annotStart -lt 0) { continue }

    # ── Sort annotation units by priority ──────────────────────────────────────
    $annotBlock  = @($lines[$annotStart..$annotEnd])
    $units       = Split-AnnotationUnits $annotBlock
    $sortedUnits = $units | Sort-Object { Get-Priority $_[0] }
    $newBlock    = @(); foreach ($u in $sortedUnits) { $newBlock += $u }

    $oldStr = $annotBlock -join "`n"
    $newStr = $newBlock   -join "`n"
    if ($oldStr -eq $newStr) { continue }   # already in correct order

    if ($DryRun) {
        Write-Host "  Would fix: $($file.Name)" -ForegroundColor Yellow
        continue
    }

    # ── Rebuild file ───────────────────────────────────────────────────────────
    $before = if ($annotStart -gt 0) { @($lines[0..($annotStart - 1)]) } else { @() }
    $after  = if (($annotEnd + 1) -lt $lines.Count) {
                  @($lines[($annotEnd + 1)..($lines.Count - 1)])
              } else { @() }

    [System.IO.File]::WriteAllLines($file.FullName, ($before + $newBlock + $after), $enc)
    Write-Host "  Fixed: $($file.Name)" -ForegroundColor Green
    $changedCount++
}

if ($DryRun) {
    Write-Host ""
    Write-Host "  Dry run complete - no files were modified." -ForegroundColor Cyan
} elseif ($changedCount -eq 0) {
    Write-Host "  All entity files already have correct annotation order." -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "  $changedCount file(s) updated." -ForegroundColor Green
}
