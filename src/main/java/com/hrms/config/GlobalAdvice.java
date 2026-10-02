package com.hrms.config;

import com.hrms.model.Employee;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the logged-in user available to every template as ${me}. */
@ControllerAdvice
public class GlobalAdvice {
    @ModelAttribute("me")
    public Employee me(HttpServletRequest req) {
        return (Employee) req.getAttribute("me");
    }
}
