package org.simple;

import org.simple.dto.Person;
import org.simple.manager.CustomEntityManager;

public class App {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "[USER]";
        String password = "[PASSWORD]";

        CustomEntityManager customEntityManager = new CustomEntityManager(url, user, password);
        Person foundPerson = customEntityManager.findById(Person.class, 1);
        System.out.println("foundPerson = " + foundPerson);
    }
}
