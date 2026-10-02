package com.hrms.controller;

import com.hrms.model.Employee;
import com.hrms.model.Role;
import com.hrms.repo.EmployeeRepository;
import com.hrms.service.FaceService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final EmployeeRepository repo;
    private final PasswordEncoder encoder;
    private final FaceService face;

    @Value("${manager.code}")
    private String managerCode;

    @GetMapping("/")
    public String root() { return "redirect:/dashboard"; }

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model m) {
        Employee e = repo.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (e == null || !encoder.matches(password, e.getPassword())) {
    m.addAttribute("err", "Invalid email or password");
    m.addAttribute("email", email);
    return "login";
}

// Block expired employees and managers
// if (e.isExited()) {
//     m.addAttribute("err",
//             "Your employment has ended. You cannot log in to the HRMS.");
//     m.addAttribute("email", email);
//     return "login";
// }

session.setAttribute("uid", e.getId());
return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email, @RequestParam String designation,
                           @RequestParam String password, @RequestParam(defaultValue = "EMPLOYEE") String role,
                           @RequestParam(name = "managerCode", defaultValue = "") String code,
                           @RequestParam(defaultValue = "") String faceDescriptors,
                           Model m, RedirectAttributes ra) {
        name = name.trim();
        email = email.trim().toLowerCase();
        designation = designation.trim();
        boolean mgr = "MANAGER".equals(role);

        String err = null;
        if (name.isEmpty() || designation.isEmpty() || password.length() < 6) err = "All fields are required; password needs 6+ characters";
        else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) err = "Invalid email address";
        else if (repo.existsByEmailIgnoreCase(email)) err = "Email is already registered";
        else if (mgr && !managerCode.equals(code)) err = "Wrong manager code";
        else err = checkFace(faceDescriptors);

        if (err != null) {
            m.addAttribute("err", err);
            m.addAttribute("name", name);
            m.addAttribute("email", email);
            m.addAttribute("designation", designation);
            m.addAttribute("role", role);
            return "register";
        }
        Employee e = new Employee();
        e.setName(name);
        e.setEmail(email);
        e.setDesignation(designation);
        e.setPassword(encoder.encode(password));
        e.setRole(mgr ? Role.MANAGER : Role.EMPLOYEE);
        e.setJoinDate(LocalDate.now());
        e.setFaceDescriptors(faceDescriptors);
        repo.save(e);
        ra.addFlashAttribute("msg", "Registered successfully. Please log in.");
        return "redirect:/login";
    }

    /** Returns an error message, or null when the captured samples are acceptable. */
    private String checkFace(String json) {
        double[][] s;
        try { s = face.parse(json); } catch (Exception ex) { return "Face capture missing or invalid - capture 3 to 5 samples"; }
        if (s.length < 3 || s.length > 5) return "Capture 3 to 5 face samples";
        for (double[] x : s)
            if (FaceService.dist(s[0], x) >= face.threshold() + 0.1) return "Samples do not look like the same person - retake them";
        for (Employee o : repo.findAll()) {
            if (o.getFaceDescriptors() == null) continue;
            double[][] stored = face.parse(o.getFaceDescriptors());
            for (double[] x : s)
                if (face.best(stored, x) < face.threshold()) return "This face is already registered on another account";
        }
        return null;
    }
}
