package com.hrms.controller;

import com.hrms.dto.Views.TeamRow;
import com.hrms.model.Employee;
import com.hrms.model.Role;
import com.hrms.repo.EmployeeRepository;
import com.hrms.service.PayrollService;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class TeamController {
    private final EmployeeRepository emps;
    private final PayrollService payroll;

    @GetMapping("/team")
    public String team(@RequestAttribute("me") Employee me, @RequestParam(required = false) String month, Model m) {
        if (me.isEmployee()) return "redirect:/dashboard";
        YearMonth ym = PayrollService.parseMonth(month);
        List<Role> roles = me.isAdmin() ? List.of(Role.MANAGER, Role.EMPLOYEE) : List.of(Role.EMPLOYEE);
        List<TeamRow> rows = emps.findByRoleInOrderByNameAsc(roles).stream()
                .map(e -> new TeamRow(e, payroll.stats(e, ym))).toList();
        m.addAttribute("rows", rows);
        m.addAttribute("month", ym.toString());
        return "team";
    }
}
