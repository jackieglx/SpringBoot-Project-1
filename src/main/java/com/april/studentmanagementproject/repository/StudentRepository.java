package com.april.studentmanagementproject.repository;

import com.april.studentmanagementproject.entity.Student;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StudentRepository {

    private static final String STUDENT_COLUMNS = "id, first_name, last_name, email_id";

    private static final RowMapper<Student> STUDENT_ROW_MAPPER = (rs, rowNum) -> new Student(
            rs.getLong("id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email_id")
    );

    private final JdbcTemplate jdbcTemplate;

    public StudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Student save(Student student) {
        if (student.getId() == null) {
            Long id = jdbcTemplate.queryForObject(
                    "INSERT INTO students (first_name, last_name, email_id) VALUES (?, ?, ?) RETURNING id",
                    Long.class,
                    student.getFirstName(),
                    student.getLastName(),
                    student.getEmail()
            );
            student.setId(id);
            return student;
        }

        jdbcTemplate.update(
                "UPDATE students SET first_name = ?, last_name = ?, email_id = ? WHERE id = ?",
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getId()
        );
        return student;
    }

    public List<Student> findAll() {
        return jdbcTemplate.query("SELECT " + STUDENT_COLUMNS + " FROM students ORDER BY id", STUDENT_ROW_MAPPER);
    }

    public Optional<Student> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + STUDENT_COLUMNS + " FROM students WHERE id = ?",
                STUDENT_ROW_MAPPER,
                id
        ).stream().findFirst();
    }

    public Optional<Student> findByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT " + STUDENT_COLUMNS + " FROM students WHERE email_id = ?",
                STUDENT_ROW_MAPPER,
                email
        ).stream().findFirst();
    }

    public List<Student> findByLastNameIgnoreCase(String lastName) {
        return jdbcTemplate.query(
                "SELECT " + STUDENT_COLUMNS + " FROM students WHERE LOWER(last_name) = LOWER(?) ORDER BY id",
                STUDENT_ROW_MAPPER,
                lastName
        );
    }

    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM students WHERE id = ?",
                Integer.class,
                id
        );
        return count != null && count > 0;
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM students WHERE id = ?", id);
    }
}
