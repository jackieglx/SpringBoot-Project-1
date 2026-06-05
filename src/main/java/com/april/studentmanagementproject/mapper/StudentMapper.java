package com.april.studentmanagementproject.mapper;

import com.april.studentmanagementproject.dto.StudentDto;
import com.april.studentmanagementproject.entity.Student;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudentMapper {

    public StudentDto toDto(Student student) {
        if (student == null) {
            return null;
        }
        return new StudentDto(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail()
        );
    }

    public Student toEntity(StudentDto studentDto) {
        if (studentDto == null) {
            return null;
        }
        Student student = new Student();
        student.setId(studentDto.getId());
        student.setFirstName(studentDto.getFirstName());
        student.setLastName(studentDto.getLastName());
        student.setEmail(studentDto.getEmail());
        return student;
    }

    public List<StudentDto> toDtoList(List<Student> students) {
        return students.stream()
                .map(this::toDto)
                .toList();
    }
}
