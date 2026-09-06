package ge.epam.gymcrm.dao;

import ge.epam.gymcrm.domain.User;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserDAO {

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Optional<User> findByUsername(String username) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    public List<String> findUsernamesStartingWith(String base) {
        return sessionFactory.getCurrentSession()
                .createQuery("SELECT u.username FROM User u WHERE u.username LIKE :base", String.class)
                .setParameter("base", base + "%")
                .list();
    }

    public void update(User user) {
        sessionFactory.getCurrentSession().merge(user);
    }
}
