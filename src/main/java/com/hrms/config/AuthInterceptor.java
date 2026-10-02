package com.hrms.config;

import com.hrms.model.Employee;
import com.hrms.repo.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final EmployeeRepository repo;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        HttpSession s = req.getSession(false);
        Long uid = s == null ? null : (Long) s.getAttribute("uid");
        Employee me = uid == null ? null : repo.findById(uid).orElse(null);
        if (me == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return false;
        }
        req.setAttribute("me", me);
        return true;
    }
}
