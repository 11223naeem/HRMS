package com.hrms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hrms.model.Attendance;
import com.hrms.model.Employee;
import com.hrms.model.LeaveRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Small view-model classes used by the templates. */
public class Views {

    @Data
    @AllArgsConstructor
    public static class Stats {
        private int presentDays;   // valid (face-verified, >= min hours) days in the month
        private int leaveMonth;    // approved leave working days deducted in this month
        private int leaveAll;      // approved leave working days, all months
        private int absentMonth;
        private int absentAll;
        private int base;
        private int deducted;
        private int net;
        private int performance;

        public String getBar() { return performance >= 80 ? "green" : performance >= 50 ? "amber" : "red"; }
    }

    @Data
    @AllArgsConstructor
    public static class TeamRow {
        private Employee emp;
        private Stats st;
    }

    @Data
    @AllArgsConstructor
    public static class LeaveView {
        private LeaveRequest req;
        private int days;
        private int deduction;
    }

    @Data
@AllArgsConstructor
public static class AttRow {
    private LocalDate day;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String status;   // PRESENT, INCOMPLETE (today, still open) or ABSENT
}
}
