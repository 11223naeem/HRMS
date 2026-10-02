package com.hrms.controller;

import com.hrms.model.Employee;
import com.hrms.model.Role;
import com.hrms.repo.EmployeeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminController {
    private final EmployeeRepository emps;

    @GetMapping("/admin/roles")
    public String roles(@RequestAttribute("me") Employee me, Model m) {
        if (!me.isAdmin()) return "redirect:/dashboard";
        m.addAttribute("people", emps.findByRoleInOrderByNameAsc(List.of(Role.MANAGER, Role.EMPLOYEE)));
        return "roles";
    }

    @PostMapping("/admin/roles")
    public String change(@RequestAttribute("me") Employee me, @RequestParam Long employeeId, @RequestParam String role,
                         RedirectAttributes ra) {
        if (!me.isAdmin()) return "redirect:/dashboard";
        Employee e = emps.findById(employeeId).orElse(null);
        if (e == null || e.isAdmin() || !(role.equals("MANAGER") || role.equals("EMPLOYEE"))) {
            ra.addFlashAttribute("err", "Invalid request");
        } else {
            e.setRole(Role.valueOf(role));
            emps.save(e);
            ra.addFlashAttribute("msg", e.getName() + " is now " + role);
        }
        return "redirect:/admin/roles";
    }

@PostMapping("/admin/activate")
public String activate(@RequestAttribute("me") Employee me,
                       @RequestParam Long employeeId,
                       RedirectAttributes ra) {

    if (!me.isAdmin()) {
        return "redirect:/dashboard";
    }

    Employee e = emps.findById(employeeId).orElse(null);

    if (e == null) {
        ra.addFlashAttribute("err", "Employee not found");
        return "redirect:/admin/roles";
    }

    if (e.isAdmin()) {
        ra.addFlashAttribute("err", "Admin cannot be activated or deactivated");
        return "redirect:/admin/roles";
    }

    e.setExitDate(null);
    emps.save(e);

    ra.addFlashAttribute(
            "msg",
            e.getName() + " has been activated successfully."
    );

    return "redirect:/admin/roles";
}

}
