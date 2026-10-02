package com.hrms.service;

import com.hrms.dto.Views.Stats;
import com.hrms.model.Attendance;
import com.hrms.model.Employee;
import com.hrms.model.LeaveRequest;
import com.hrms.model.LeaveStatus;
import com.hrms.repo.AttendanceRepository;
import com.hrms.repo.LeaveRequestRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayrollService {
    private final LeaveRequestRepository leaves;
    private final AttendanceRepository atts;

    @Value("${pay.rate}")
    private int rate;
    @Value("${attendance.min.hours}")
    private double minHours;
    @Value("${perf.penalty}")
    private int penalty;

    public int rate() { return rate; }

    public static boolean isWorkday(LocalDate d) { return d.getDayOfWeek().getValue() <= 5; }

    /** Monday-Friday days between from and to, inclusive. */
    public static int workingDays(LocalDate from, LocalDate to) {
        int n = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) if (isWorkday(d)) n++;
        return n;
    }

    public static YearMonth parseMonth(String s) {
        try { return YearMonth.parse(s); } catch (Exception e) { return YearMonth.now(); }
    }

    /** Valid day = check-in and check-out both recorded and at least minHours apart. */
    public boolean valid(Attendance a) {
        return a.getLoginTime() != null && a.getLogoutTime() != null
                && Duration.between(a.getLoginTime(), a.getLogoutTime()).toMinutes() >= minHours * 60L;
    }

    public Stats stats(Employee e, YearMonth ym) {
        Set<LocalDate> leaveDays = new HashSet<>();
        int leaveAll = 0, leaveMonth = 0;
        for (LeaveRequest l : leaves.findByEmployeeAndStatus(e, LeaveStatus.APPROVED)) {
            int wd = workingDays(l.getFromDate(), l.getToDate());
            leaveAll += wd;
            if (l.getDecidedOn() != null && YearMonth.from(l.getDecidedOn()).equals(ym)) leaveMonth += wd;
            for (LocalDate d = l.getFromDate(); !d.isAfter(l.getToDate()); d = d.plusDays(1))
                if (isWorkday(d)) leaveDays.add(d);
        }
        Set<LocalDate> okDays = atts.findByEmployee(e).stream().filter(this::valid)
                .map(Attendance::getDay).collect(Collectors.toSet());
        int present = (int) okDays.stream().filter(d -> YearMonth.from(d).equals(ym)).count();
        int absMonth = absent(e, ym.atDay(1), ym.atEndOfMonth(), leaveDays, okDays);
        int absAll = absent(e, e.getJoinDate(), LocalDate.now(), leaveDays, okDays);

        int base = ym.lengthOfMonth() * rate;
        int deducted = Math.min(base, (leaveMonth + absMonth) * rate);
        int perf = Math.max(0, 100 - penalty * (leaveMonth + absMonth));
        return new Stats(present, leaveMonth, leaveAll, absMonth, absAll, base, deducted, base - deducted, perf);
    }

    /** Mon-Fri days from joining date up to YESTERDAY with no approved leave and no valid attendance. */
    private int absent(Employee e, LocalDate from, LocalDate to, Set<LocalDate> leave, Set<LocalDate> ok) {
        LocalDate end = LocalDate.now().minusDays(1);
        if (to.isBefore(end)) end = to;
        if (e.getExitDate() != null && e.getExitDate().isBefore(end)) end = e.getExitDate();
        LocalDate start = from.isBefore(e.getJoinDate()) ? e.getJoinDate() : from;
        int n = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1))
            if (isWorkday(d) && !leave.contains(d) && !ok.contains(d)) n++;
        return n;
    }

/** Past Mon-Fri days (up to yesterday) that count as absent. Same rule as stats(). */
public List<LocalDate> absentDates(Employee e) {
    Set<LocalDate> leave = new HashSet<>();
    for (LeaveRequest l : leaves.findByEmployeeAndStatus(e, LeaveStatus.APPROVED))
        for (LocalDate d = l.getFromDate(); !d.isAfter(l.getToDate()); d = d.plusDays(1)) leave.add(d);
    Set<LocalDate> ok = atts.findByEmployee(e).stream().filter(this::valid)
            .map(Attendance::getDay).collect(Collectors.toSet());
    LocalDate end = LocalDate.now().minusDays(1);
    if (e.getExitDate() != null && e.getExitDate().isBefore(end)) end = e.getExitDate();
    List<LocalDate> out = new ArrayList<>();
    for (LocalDate d = e.getJoinDate(); !d.isAfter(end); d = d.plusDays(1))
        if (isWorkday(d) && !leave.contains(d) && !ok.contains(d)) out.add(d);
    return out;
}

}
