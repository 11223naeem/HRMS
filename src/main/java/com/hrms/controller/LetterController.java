package com.hrms.controller;

import com.hrms.model.Employee;
import com.hrms.model.Letter;
import com.hrms.model.LetterType;
import com.hrms.model.Role;
import com.hrms.repo.EmployeeRepository;
import com.hrms.repo.LetterRepository;
import com.hrms.service.PayrollService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
public class LetterController {
    private final LetterRepository letters;
    private final EmployeeRepository emps;
    private final PayrollService payroll;

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private List<Employee> people(Employee me) {
        return emps.findByRoleInOrderByNameAsc(me.isAdmin() ? List.of(Role.MANAGER, Role.EMPLOYEE) : List.of(Role.EMPLOYEE));
    }

    @GetMapping("/letters")
    public String list(@RequestAttribute("me") Employee me, Model m) {
        m.addAttribute("mine", letters.findByEmployeeOrderByIdDesc(me));
        if (!me.isEmployee()) {
            m.addAttribute("people", people(me));
            m.addAttribute("issued", letters.findAllByOrderByIdDesc().stream()
                    .filter(l -> me.isAdmin() || l.getEmployee().getRole() == Role.EMPLOYEE).toList());
        }
        return "letters";
    }

   @PostMapping("/letters/issue")
public String issue(@RequestAttribute("me") Employee me,
                    @RequestParam Long employeeId,
                    @RequestParam LetterType type,
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate joinDate,
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate exitDate,
                    RedirectAttributes ra) {

    if (me.isEmployee() || me.isExited()) {
        return "redirect:/dashboard";
    }

    Employee e = emps.findById(employeeId).orElse(null);
   // Do not allow any new letter for an already expired employee
if (e != null && !e.isAdmin() && e.isExited()) {
    ra.addFlashAttribute(
            "err",
            "This employee has already exited. Activate the employee first.");
    return "redirect:/letters";
}
        if (e == null || e.isAdmin() || (!me.isAdmin() && !e.isEmployee())) {
            ra.addFlashAttribute("err", "You cannot issue a letter to that person");
            return "redirect:/letters";
        }
        Letter l = new Letter();
        l.setEmployee(e);
        l.setType(type);
        l.setIssuedOn(LocalDate.now());
        if (type == LetterType.OFFER) {
            if (joinDate != null) e.setJoinDate(joinDate);
            l.setContent("Dear " + e.getName() + ",\n\n"
                    + "We are pleased to offer you the position of " + e.getDesignation() + " with our organisation.\n\n"
                    + "Your date of joining is " + e.getJoinDate().format(F) + ". Your compensation is Rs " + payroll.rate()
                    + " per working day (Monday to Friday). Monthly salary is the number of working days in the month "
                    + "multiplied by this rate, less deductions for approved leave and absence.\n\n"
                    + "We look forward to working with you.\n\nSincerely,\nHR Department");
        } else {
            LocalDate ex = exitDate != null ? exitDate : LocalDate.now();
            if (ex.isBefore(e.getJoinDate())) {
                ra.addFlashAttribute("err", "End of employment cannot be before the joining date");
                return "redirect:/letters";
            }
            e.setExitDate(ex);
            l.setContent("Dear " + e.getName() + ",\n\n"
                    + "This letter confirms that your employment as " + e.getDesignation() + " with our organisation ends on "
                    + ex.format(F) + ".\n\n"
                    + "Your salary will be settled up to this date. Please return all company property before your last day.\n\n"
                    + "We thank you for your service and wish you every success.\n\nSincerely,\nHR Department");
        }
        emps.save(e);
        letters.save(l);
        ra.addFlashAttribute("msg", (type == LetterType.OFFER ? "Offer" : "Expiration") + " letter issued to " + e.getName());
        return "redirect:/letters";
    }

    @GetMapping("/letters/{id}")
    public String open(@RequestAttribute("me") Employee me, @PathVariable Long id, Model m) {
        Letter l = letters.findById(id).orElse(null);
        if (l == null || (me.isEmployee() && !l.getEmployee().getId().equals(me.getId()))) return "redirect:/letters";
        m.addAttribute("l", l);
        m.addAttribute("issuedOn", l.getIssuedOn().format(F));
        return "letter";
    }
}
