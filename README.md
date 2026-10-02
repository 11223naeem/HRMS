# HRMS (Spring Boot 3.3, Thymeleaf, MySQL, face-api.js)

## Run
1. Install JDK 17+ and Maven 3.9+, and start MySQL 8 (the `hrms` database is created automatically).
2. Edit `src/main/resources/application.properties`: MySQL username/password, and `office.lat` / `office.lng`.
3. `mvn spring-boot:run`, then open **http://localhost:8080** (camera + GPS need localhost or HTTPS).
4. Admin login: `admin@hrms.com` / `Admin@123` (from application.properties). Manager code: `MGR2026`.

## Office coordinates
Open http://localhost:8080 in Chrome, press F12, Console tab, and run:
`navigator.geolocation.getCurrentPosition(p => console.log(p.coords.latitude, p.coords.longitude))`
Paste the two numbers into office.lat / office.lng and restart. (Or right-click your spot in Google Maps.)

## Test scenarios
Sept 2026 has 22 working days -> base Rs 22,000.
* 16-day leave: employee applies Mon 2026-09-07 to Mon 2026-09-28 (16 working days); a manager approves -> deducted Rs 16,000, net Rs 6,000.
* Weekend never counts: apply Fri 2026-10-02 to Mon 2026-10-05 -> shows 2 working days / Rs 2,000.
* Absent weekday: `UPDATE hrms.employee SET join_date = CURDATE() - INTERVAL 1 DAY WHERE email='emp@x.com';`
  (yesterday must be Mon-Fri) -> 1 absent day: deducted Rs 1,000, performance 98%.
* Valid 6-hour day: check in, then `UPDATE hrms.attendance SET login_time = login_time - INTERVAL 7 HOUR;` and check out.
* Face mismatch: log in as user A and let user B check in (or set face.threshold=0.01) -> "Face not recognised".
* Outside 100 m: change office.lat/lng to another place (or Chrome DevTools > Sensors > Location) -> "Too far from office (... m)".
* Roles: register a manager with the code; a wrong code is refused; admin can promote/demote on the Roles page.
