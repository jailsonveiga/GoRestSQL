package com.careerdevs.gorestsql.repos;

import com.careerdevs.gorestsql.models.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Integer> {

}
