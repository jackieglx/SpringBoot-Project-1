package com.april.studentmanagementproject.repository;

import com.april.studentmanagementproject.entity.Student;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class StudentRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Student save(Student student) {
        Session session = currentSession();

        if (student.getId() == null) {
            session.persist(student);
            return student;
        }

        return session.merge(student);
    }

    public List<Student> findAll() {
        return currentSession()
                .createQuery("from Student order by id", Student.class)
                .getResultList();
    }

    public Optional<Student> findById(Long id) {
        return Optional.ofNullable(currentSession().find(Student.class, id));
    }

    public Optional<Student> findByEmail(String email) {
        return currentSession()
                .createQuery("from Student where email = :email", Student.class)
                .setParameter("email", email)
                .uniqueResultOptional();
    }

    public boolean existsById(Long id) {
        Long count = currentSession()
                .createQuery("select count(s) from Student s where s.id = :id", Long.class)
                .setParameter("id", id)
                .uniqueResult();

        return count != null && count > 0;
    }

    @Transactional
    public void deleteById(Long id) {
        findById(id).ifPresent(currentSession()::remove);
    }

    private Session currentSession() {
        return entityManager.unwrap(Session.class);
    }
}
