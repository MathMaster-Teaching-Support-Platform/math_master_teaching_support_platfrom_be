package com.fptu.math_master.enums;

/**
 * Represents the lifecycle stages of a course.
 */
public enum CourseStatus {
    /** Teacher is still editing the course content. Not visible to students or admins. */
    DRAFT,
    
    /** Teacher has submitted the course for administrative review. */
    PENDING_REVIEW,
    
    /** Admin has approved the course. visible to all students. */
    PUBLISHED,
    
    /** Admin has rejected the course. Teacher needs to make changes and submit again. */
    REJECTED
}
