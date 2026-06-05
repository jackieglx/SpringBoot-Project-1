package com.april.studentmanagementproject.service;

import com.april.studentmanagementproject.dto.StudentDto;

import java.util.List;

public interface StudentService {

    StudentDto addStudent(StudentDto studentDto);

    List<StudentDto> getAllStudents();

    StudentDto getStudentById(Long id);

    StudentDto updateStudent(Long id, StudentDto studentDto);
}
