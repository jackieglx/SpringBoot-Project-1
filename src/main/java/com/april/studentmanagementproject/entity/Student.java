package com.april.studentmanagementproject.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;
}
