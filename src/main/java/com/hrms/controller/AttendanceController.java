package com.hrms.controller;

import com.hrms.dto.Views.AttRow;
import com.hrms.model.Attendance;
import com.hrms.model.Employee;
import com.hrms.repo.AttendanceRepository;
import com.hrms.service.FaceService;
import com.hrms.service.GeoUtil;
import com.hrms.service.PayrollService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Controller
@RequiredArgsConstructor
@RequestMapping("/attendance")
public class AttendanceController {
    private final AttendanceRepository atts;
    private final FaceService face;
    private final PayrollService payroll;

    @Value("${office.lat}")
    private double officeLat;
    @Value("${office.lng}")
    private double officeLng;
    @Value("${office.radius}")
    private double radius;
    @Value("${attendance.min.hours}")
    private double minHours;

    public record AttReq(double[] descriptor, double lat, double lng) {}

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm:ss");

   @GetMapping
public String page(@RequestAttribute("me") Employee me, Model m) {
    if (me.isAdmin()) return "redirect:/dashboard";
    Set<LocalDate> absent = new HashSet<>(payroll.absentDates(me));
    List<AttRow> rows = new ArrayList<>();
    Set<LocalDate> seen = new HashSet<>();
    for (Attendance a : atts.findByEmployee(me)) {
        seen.add(a.getDay());
        String status = payroll.valid(a) ? "PRESENT" : absent.contains(a.getDay()) ? "ABSENT" : "INCOMPLETE";
        rows.add(new AttRow(a.getDay(), a.getLoginTime(), a.getLogoutTime(), status));
    }
    for (LocalDate d : absent)
        if (!seen.contains(d)) rows.add(new AttRow(d, null, null, "ABSENT"));   // no check-in at all
    rows.sort(Comparator.comparing(AttRow::getDay).reversed());

    m.addAttribute("today", atts.findByEmployeeAndDay(me, LocalDate.now()).orElse(null));
    m.addAttribute("rows", rows.stream().limit(31).toList());
    m.addAttribute("radius", (int) radius);
    m.addAttribute("minHours", minHours);
    return "attendance";
}

    @PostMapping("/checkin")
    @ResponseBody
    public Map<String, Object> checkin(@RequestAttribute("me") Employee me, @RequestBody AttReq r) {
        
        return mark(me, r, true);
    }

    @PostMapping("/checkout")
    @ResponseBody
    public Map<String, Object> checkout(@RequestAttribute("me") Employee me, @RequestBody AttReq r) {
        return mark(me, r, false);
    }

    private Map<String, Object> mark(Employee me, AttReq r, boolean in) {
        if (me.isAdmin()) return res(false, "Admin has no attendance");
        if (me.isExited()) {
        return res(false, "Your employment has ended. You cannot mark attendance.");
    }
        if (me.getFaceDescriptors() == null || r.descriptor() == null || r.descriptor().length != 128)
            return res(false, "Invalid face data");

        // (a) face must match the LOGGED-IN user
        double score = face.best(face.parse(me.getFaceDescriptors()), r.descriptor());
        if (score >= face.threshold())
            return res(false, "Face not recognised (distance " + String.format("%.2f", score) + ")");

        // (b) must be inside the office radius
        double meters = GeoUtil.meters(r.lat(), r.lng(), officeLat, officeLng);
        if (meters > radius)
            return res(false, "Too far from office (" + Math.round(meters) + " m). Must be within " + (int) radius + " m");

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        Attendance a = atts.findByEmployeeAndDay(me, today).orElse(null);

        if (in) {
            if (a != null && a.getLoginTime() != null)
                return res(true, "Already checked in today at " + a.getLoginTime().format(HM));
            if (a == null) { a = new Attendance(); a.setEmployee(me); a.setDay(today); }
            a.setLoginTime(now);
            a.setFaceScoreIn(score);
            atts.save(a);
            return res(true, "Checked in at " + now.format(HM));
        }

        if (a == null || a.getLoginTime() == null) return res(false, "Check in first");
        a.setLogoutTime(now);   // a later check-out replaces the earlier one
        a.setFaceScoreOut(score);
        atts.save(a);
        Duration d = Duration.between(a.getLoginTime(), now);
        String worked = d.toHours() + " h " + d.toMinutesPart() + " m";
        if (!payroll.valid(a))
            return res(true, "Checked out at " + now.format(HM) + " - only " + worked + " worked, need " + minHours
                    + " h, so today will count as ABSENT. Check out again later to replace this.");
        return res(true, "Checked out at " + now.format(HM) + " (" + worked + "). Day counted as present.");
    }

    private static Map<String, Object> res(boolean ok, String msg) {
        return Map.of("ok", ok, "message", msg);
    }
}
