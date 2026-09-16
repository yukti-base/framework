-- Manual schema commands for the academic-fields rework (2026-09-11).
-- Hibernate's ddl-auto=update will auto-ADD the new columns below on next
-- app startup in local/dev, but it never drops columns -- run the DROP
-- statements yourself wherever you want the superseded columns actually
-- gone (they're otherwise just orphaned, unused, harmless).

-- UserRoleAssignment: new admin-set academic-program field.
ALTER TABLE user_role_assignments ADD COLUMN degree VARCHAR(255);

-- StudentProfile: drop the free-text institution/degree/branch columns --
-- superseded by College.name / Department.name (via the student's active
-- UserRoleAssignment) and UserRoleAssignment.degree.
ALTER TABLE student_profiles DROP COLUMN institution;
ALTER TABLE student_profiles DROP COLUMN degree;
ALTER TABLE student_profiles DROP COLUMN branch;

-- StudentProfile: new semester-wise SGPA columns (admin-set only, via
-- admin-service's bulk-student-import -- StudentRequest already accepted
-- these field names, they were just silently discarded before this change).
ALTER TABLE student_profiles ADD COLUMN sem1_gpa DOUBLE PRECISION;
ALTER TABLE student_profiles ADD COLUMN sem2_gpa DOUBLE PRECISION;
ALTER TABLE student_profiles ADD COLUMN sem3_gpa DOUBLE PRECISION;
ALTER TABLE student_profiles ADD COLUMN sem4_gpa DOUBLE PRECISION;
ALTER TABLE student_profiles ADD COLUMN sem5_gpa DOUBLE PRECISION;
ALTER TABLE student_profiles ADD COLUMN sem6_gpa DOUBLE PRECISION;
ALTER TABLE student_profiles ADD COLUMN sem7_gpa DOUBLE PRECISION;
ALTER TABLE student_profiles ADD COLUMN sem8_gpa DOUBLE PRECISION;
