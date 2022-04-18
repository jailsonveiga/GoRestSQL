package com.careerdevs.gorestsql.controllers;

import com.careerdevs.gorestsql.models.User;
import com.careerdevs.gorestsql.repos.UserRepository;
import com.careerdevs.gorestsql.utils.ApiErrorHandling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/*
                Required Routes for GoRestSQL MVP:
                    Get route that returns one user by ID from the SQL database
                    Get route that returns all users stored in the SQL database
                    Delete route that deletes one user by ID from SQL database (returns the delete SQL user data)
                    Delete route that deletes all users from SQL database (returns how many users were deleted)
                    Post route that requires one user by ID from GoRest and saves their data to your local database (returns the SQL user data)
                    Post route that uploads all users from the GoRest API into the SQL database (returns how many users were uploaded)
                    Post route that create a user on just the SQL database (returns the newly created SQL user data)
                    Put route that updates a user on just the SQL database (returns the updated SQL user data)


 */

@RestController
@RequestMapping ("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById (@PathVariable("id") String id){
        try {

            if (ApiErrorHandling.isStrNaN(id)) {

                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, id + " is not a valid ID");

            }

            int uID = Integer.parseInt(id);

            Optional<User> foundUser = userRepository.findById(uID);

            if (foundUser.isEmpty()) {

                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, " User Not Found With ID: " + id);

            }

            return new ResponseEntity<>(foundUser, HttpStatus.OK);

        } catch (HttpClientErrorException e){

            return ApiErrorHandling.customApiError(e.getMessage(), e.getStatusCode());

        } catch (Exception e) {

            return ApiErrorHandling.genericApiError(e);

        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserById(@PathVariable ("id") String id) {
        try{
            if (ApiErrorHandling.isStrNaN(id)) {
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, id + " is not a valid ID");
            }
            int uID = Integer.parseInt(id);

            Optional<User> foundUser = userRepository.findById(uID);

            if(foundUser.isEmpty()) {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, " User Not Found With Id: " + id);
            }
            userRepository.deleteById(uID);

            return new ResponseEntity<>(foundUser, HttpStatus.OK);
        } catch(HttpClientErrorException e) {

            return ApiErrorHandling.customApiError(e.getMessage(), e.getStatusCode());

        } catch (Exception e) {

            return ApiErrorHandling.genericApiError(e);

        }
    }

    @DeleteMapping ("/deleteall")
    public ResponseEntity<?> deleteAllUsers() {
        try {

            long totalUsers = userRepository.count();
            userRepository.deleteAll();

            return new ResponseEntity<>("Users Delete: " + totalUsers, HttpStatus.OK);

        } catch (HttpClientErrorException e) {

            return ApiErrorHandling.genericApiError(e);

        }
    }

    @PostMapping("/upload/{id}")
    public ResponseEntity<?> uploadUserById (
            @PathVariable ("id") String userId,
            RestTemplate restTemplate
    ) {
        try {

            if(ApiErrorHandling.isStrNaN(userId)) {

                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, userId + " is not a valid ID");

            }

            int uID = Integer.parseInt(userId);

            String url = "https://gorest.co.in/public/v2/users/" + uID;

            User foundUser = restTemplate.getForObject(url, User.class);

             if(foundUser == null) {

                 throw new HttpClientErrorException(HttpStatus.NOT_FOUND, " User with ID: " + uID + " not found");

             }

            User savedUser = userRepository.save(foundUser);

            return new ResponseEntity<>(savedUser, HttpStatus.CREATED);

        } catch (HttpClientErrorException e) {

            return ApiErrorHandling.customApiError(e.getMessage(), e.getStatusCode());

        } catch (Exception e) {

            return ApiErrorHandling.genericApiError(e);

        }
    }

    @PostMapping("/")
    public ResponseEntity<?> createNewUser (@RequestBody User newUser) {
        try {

            User savedUser = userRepository.save(newUser);

            return new ResponseEntity<>(savedUser, HttpStatus.CREATED);

        } catch(HttpClientErrorException e) {

            return ApiErrorHandling.customApiError(e.getMessage(), e.getStatusCode());

        } catch (Exception e) {

            return ApiErrorHandling.genericApiError(e);

        }
    }

    @PutMapping("/")
    public ResponseEntity<?> updateUser (@RequestBody User updateUser) {
        try{

            User savedUser = userRepository.save(updateUser);

            return new ResponseEntity<>(savedUser, HttpStatus.OK);

        } catch(HttpClientErrorException e) {

            return ApiErrorHandling.customApiError(e.getMessage(), e.getStatusCode());

        } catch (Exception e) {

            return ApiErrorHandling.genericApiError(e);

        }
    }

    @PostMapping("/uploadall")
    public ResponseEntity<?> uploadAll(RestTemplate restTemplate) {
        try{

            String url = "https://gorest.co.in/public/v2/users";

            ResponseEntity<User[]> response = restTemplate.getForEntity(url, User[].class);

            User[] firstPageUsers = response.getBody();

            if(firstPageUsers == null) {

                throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Get first page of " + "users from GoREST");

            }

            ArrayList<User> allUsers = new ArrayList<>(Arrays.asList(firstPageUsers));

            HttpHeaders responseHeaders = response.getHeaders();

            String totalPages = Objects.requireNonNull(responseHeaders.get("X-Pagination-Pages")).get(0);
            int totalPgNum = Integer.parseInt(totalPages);

            for(int i = 2; i <= totalPgNum; i++) {
                String pageUrl = url + "?page=" + i;
                User[] pageUsers = restTemplate.getForObject(pageUrl, User[].class);

                if(pageUsers == null) {

                    throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Get page " + i + " of users from GoREST");

                }

                allUsers.addAll(Arrays.asList(firstPageUsers));
            }

            //upload all users to SQL
            userRepository.saveAll(allUsers);

            return new ResponseEntity<>("Users Created: " + allUsers.size(), HttpStatus.OK);

        } catch (HttpClientErrorException e){

            return ApiErrorHandling.customApiError(e.getMessage(), e.getStatusCode());

        }  catch (Exception e) {

            return ApiErrorHandling.genericApiError(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUser () {
        try{

            Iterable<User> allUsers = userRepository.findAll();
            return new ResponseEntity<>(allUsers, HttpStatus.OK);

        } catch (Exception e) {

          return ApiErrorHandling.genericApiError(e);

        }
    }

}
