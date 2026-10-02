package com.hrms.controller;

import com.hrms.dto.Views.LeaveView;
import com.hrms.model.Employee;
import com.hrms.model.LeaveRequest;
import com.hrms.model.LeaveStatus;
import com.hrms.model.Role;
import com.hrms.repo.LeaveRequestRepository;
import com.hrms.service.PayrollService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class LeaveController {
    private final LeaveRequestRepository leaves;
    private final PayrollService payroll;

    private List<LeaveView> view(List<LeaveRequest> list) {
        return list.stream().map(l -> {
            int d = PayrollService.workingDays(l.getFromDate(), l.getToDate());
            return new LeaveView(l, d, d * payroll.rate());
        }).toList();
    }

    @GetMapping("/leaves")
    public String mine(@RequestAttribute("me") Employee me, Model m) {
        if (me.isAdmin()) return "redirect:/dashboard";
        m.addAttribute("items", view(leaves.findByEmployeeOrderByIdDesc(me)));
        return "leaves";
    }

   @PostMapping("/leaves/apply")
public String apply(@RequestAttribute("me") Employee me,
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                    @RequestParam String reason,
                    RedirectAttributes ra) {

    if (me.isAdmin() || me.isExited()) {
    ra.addFlashAttribute("err",
            "Your employment has ended. You cannot apply for leave.");
    return "redirect:/dashboard";
}

    if (toDate.isBefore(fromDate)) {
        ra.addFlashAttribute("err", "End date must not be before the start date");
    }
    else if (PayrollService.workingDays(fromDate, toDate) == 0) {
        ra.addFlashAttribute("err", "That range has no working days (Mon-Fri)");
    }
    else if (reason.isBlank()) {
        ra.addFlashAttribute("err", "Please give a reason");
    }
    else {

        // Check for duplicate / overlapping leave
        List<LeaveRequest> overlapping = leaves.findOverlappingLeaves(
                me,
                fromDate,
                toDate,
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED)
        );

        if (!overlapping.isEmpty()) {
            ra.addFlashAttribute(
                    "err",
                    "Duplicate leave days. You already have a leave request for these dates."
            );
        }
        else {
            LeaveRequest l = new LeaveRequest();
            l.setEmployee(me);
            l.setFromDate(fromDate);
            l.setToDate(toDate);
            l.setReason(reason.trim());

            leaves.save(l);

            ra.addFlashAttribute("msg", "Leave request submitted");
        }
    }

    return "redirect:/leaves";
}

    @GetMapping("/approvals")
    public String approvals(@RequestAttribute("me") Employee me, Model m) {
        if (me.isEmployee()) return "redirect:/dashboard";
        Role target = me.isAdmin() ? Role.MANAGER : Role.EMPLOYEE;   // manager leave -> admin; employee leave -> managers
        m.addAttribute("items", view(leaves.findByStatusAndEmployeeRoleOrderByIdAsc(LeaveStatus.PENDING, target)));
        return "approvals";
    }

    @PostMapping("/approvals/{id}/{action}")
    public String decide(@RequestAttribute("me") Employee me, @PathVariable Long id, @PathVariable String action,
                         RedirectAttributes ra) {
        if (me.isEmployee()) return "redirect:/dashboard";
        LeaveRequest l = leaves.findById(id).orElse(null);
        if (l.getEmployee().isExited()) {
    return back(ra, "err",
            "This employee has already exited. Activate the employee first.");
}

        if (l == null || l.getStatus() != LeaveStatus.PENDING) return back(ra, "err", "Request not found or already decided");
        if (!me.isAdmin() && l.getEmployee().getRole() != Role.EMPLOYEE)
            return back(ra, "err", "Managers can decide only employee leaves");
        if (l.getEmployee().getId().equals(me.getId())) return back(ra, "err", "You cannot decide your own leave");

        boolean approve = "approve".equals(action);
        l.setStatus(approve ? LeaveStatus.APPROVED : LeaveStatus.REJECTED);
        l.setDecidedOn(LocalDate.now());
        leaves.save(l);
        int days = PayrollService.workingDays(l.getFromDate(), l.getToDate());
        return back(ra, "msg", approve
                ? "Approved. Rs " + (days * payroll.rate()) + " (" + days + " working days) will be deducted this month."
                : "Leave rejected. No deduction.");
    }

    private String back(RedirectAttributes ra, String key, String msg) {
        ra.addFlashAttribute(key, msg);
        return "redirect:/approvals";
    }
}
