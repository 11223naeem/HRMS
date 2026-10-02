package com.hrms.controller;

import com.hrms.model.Employee;
import com.hrms.model.LeaveStatus;
import com.hrms.model.Role;
import com.hrms.repo.AttendanceRepository;
import com.hrms.repo.EmployeeRepository;
import com.hrms.repo.LeaveRequestRepository;
import com.hrms.repo.LetterRepository;
import com.hrms.service.PayrollService;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final PayrollService payroll;
    private final AttendanceRepository atts;
    private final LetterRepository letters;
    private final EmployeeRepository emps;
    private final LeaveRequestRepository leaves;

    @GetMapping("/dashboard")
    public String dashboard(@RequestAttribute("me") Employee me, @RequestParam(required = false) String month, Model m) {
        YearMonth ym = PayrollService.parseMonth(month);
        m.addAttribute("month", ym.toString());
        if (me.isAdmin()) {
            m.addAttribute("nEmp", emps.countByRole(Role.EMPLOYEE));
            m.addAttribute("nMgr", emps.countByRole(Role.MANAGER));
            m.addAttribute("nPending", leaves.countByStatusAndEmployeeRole(LeaveStatus.PENDING, Role.MANAGER));
            m.addAttribute("nLetters", letters.count());
            return "admin-home";
        }
        m.addAttribute("s", payroll.stats(me, ym));
        m.addAttribute("today", atts.findByEmployeeAndDay(me, LocalDate.now()).orElse(null));
        m.addAttribute("myLetters", letters.findTop5ByEmployeeOrderByIdDesc(me));
        return "dashboard";
    }
}
