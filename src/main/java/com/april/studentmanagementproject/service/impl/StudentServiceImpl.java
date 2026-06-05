package com.april.studentmanagementproject.service.impl;

import com.april.studentmanagementproject.dto.StudentDto;
import com.april.studentmanagementproject.entity.Student;
import com.april.studentmanagementproject.exception.ResourceNotFoundException;
import com.april.studentmanagementproject.mapper.StudentMapper;
import com.april.studentmanagementproject.repository.StudentRepository;
import com.april.studentmanagementproject.service.StudentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    public StudentServiceImpl(StudentRepository studentRepository, StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
    }

    @Override
    public StudentDto addStudent(StudentDto studentDto) {
        if (studentRepository.findByEmail(studentDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + studentDto.getEmail());
        }

        Student student = studentMapper.toEntity(studentDto);
        student.setId(null);

        Student savedStudent = studentRepository.save(student);
        return studentMapper.toDto(savedStudent);
    }

    @Override
    public List<StudentDto> getAllStudents() {
        return studentMapper.toDtoList(studentRepository.findAll());
    }

    @Override
    public StudentDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
        return studentMapper.toDto(student);
    }
}
