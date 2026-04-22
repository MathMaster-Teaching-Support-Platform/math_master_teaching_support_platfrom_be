# FE Integration Guide: Lesson Slide Generation

Tai lieu nay mo ta nhung thay doi FE can xu ly trong luong tao slide bai hoc.

---

## 1. Van de hien tai

Preview dang hien thi noi dung raw LaTeX text (field `content`) thay vi anh da render.

Backend da gui dung — field `previewImageUrl` co URL anh PNG da render boi QuickLaTeX.
FE chua dung field nay, van render `content` thang ra man hinh.

---

## 2. Schema response cua POST /api/lesson-slides/generate-content

Moi item trong mang `slides` tra ve:

```
slideNumber     : number
slideType       : string
heading         : string
content         : string   -- raw text hoac raw LaTeX, dung cho o chinh sua
previewImageUrl : string | null
```

Quy tac `previewImageUrl`:

- `PLAIN_TEXT` / `HYBRID` mode: luon la `null`
- `LATEX` mode: la URL anh PNG da render boi QuickLaTeX, khong phai `null`

---

## 3. FE can chinh gi

### 3.1 Vung preview (hien thi cho giao vien xem)

Hien tai FE dang lam:

```
hien thi: slide.content
```

Can doi thanh:

```
neu slide.previewImageUrl != null:
    hien thi: <img src={slide.previewImageUrl} />
neu slide.previewImageUrl == null:
    hien thi: slide.content (text thuong)
```

### 3.2 O chinh sua (textarea / editor)

Giu nguyen nhu cu — luon bind vao `slide.content`.
`content` la raw text/LaTeX ma giao vien co the chinh sua truoc khi gen PPTX.
Khong thay doi phan nay.

### 3.3 Khi goi generate-pptx-from-json

Gui `slide.content` (da chinh sua) vao request body, KHONG gui `previewImageUrl`.
Backend se tu render lai anh moi khi tao PPTX.

---

## 4. Pseudocode React

```
function SlidePreview({ slide }) {
  return (
    <div>
      <h3>{slide.heading}</h3>

      {slide.previewImageUrl
        ? <img src={slide.previewImageUrl} alt="slide preview" style={{ maxWidth: '100%' }} />
        : <p style={{ whiteSpace: 'pre-wrap' }}>{slide.content}</p>
      }
    </div>
  );
}

function SlideEditor({ slide, onChange }) {
  // Luon dung content cho editor, du la LATEX mode
  return (
    <textarea
      value={slide.content}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}
```

---

## 5. Luu y them

- `previewImageUrl` la URL external (quicklatex.com), khong can proxy.
- URL co the thay doi moi lan goi generate-content vi cache co TTL.
  FE khong nen luu URL nay vao local storage lau dai.
- Khi outputFormat la `PLAIN_TEXT` hoac `HYBRID`, mang `slides` van co field `previewImageUrl`
  nhung gia tri la `null`. FE can xu ly null truoc khi render.

---

## 6. outputFormat la gi

Field `outputFormat` duoc gui trong request de chi dinh che do sinh noi dung:

| Gia tri      | Y nghia                                        |
| ------------ | ---------------------------------------------- |
| `PLAIN_TEXT` | Van ban thuan, khong co LaTeX (mac dinh)       |
| `HYBRID`     | Van ban tieng Viet + LaTeX cho cong thuc       |
| `LATEX`      | Toan bo LaTeX. Slide duoc render thanh anh PNG |

FE gui `outputFormat` o 2 cho:

1. POST /api/lesson-slides/generate-content — de AI sinh dung kieu noi dung
2. POST /api/lesson-slides/generate-pptx-from-json — de backend biet cach xu ly slide khi tao PPTX

---

## 7. Endpoint moi: DELETE /api/lesson-slides/generated/{id}

Xoa file slide da generate.

Auth: Bearer token role TEACHER (chi xoa duoc file cua chinh minh) hoac ADMIN.

Response thanh cong:

```json
{
  "code": 1000,
  "message": "Generated slide deleted successfully",
  "result": null
}
```

Loi 403 neu khong phai owner.
