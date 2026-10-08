package in.opt.sfa.tenant.dcr.view.repository;

import in.opt.sfa.tenant.dcr.view.entity.Dcr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DcrRepository extends JpaRepository<Dcr, Long> {

    /**
     * Division-first filtered report row set. Division is resolved via
     * client -> route -> division (route already carries division_oid), and
     * country via client -> route -> state -> country (drives Nepal date choice).
     */
    @Query(value = """
            SELECT d.oid AS oid,
                   d.dcr_date AS dcrDate,
                   d.dcr_date_np AS dcrDateNp,
                   d.work_type AS workType,
                   d.remarks AS remarks,
                   d.time_in AS timeIn,
                   d.time_out AS timeOut,
                   e.emp_name AS empName,
                   c.name AS clientName,
                   ct.type_name AS clientTypeName,
                   ar.area_name AS areaName,
                   r.route_name AS routeName,
                   dv.division_name AS divisionName,
                   co.country_name AS countryName
            FROM dcr d
            JOIN employee e ON e.oid = d.employee_oid
            JOIN client c ON c.oid = d.client_oid
            LEFT JOIN client_type ct ON ct.oid = c.client_type_oid
            LEFT JOIN city_master ar ON ar.oid = c.area_oid
            LEFT JOIN route r ON r.oid = c.route_oid
            LEFT JOIN division_master dv ON dv.oid = r.division_oid
            LEFT JOIN state_master st ON st.oid = r.state_oid
            LEFT JOIN country_master co ON co.oid = st.country_oid
            WHERE d.status = 1
              AND (:divisionOid IS NULL OR r.division_oid = :divisionOid)
              AND (:routeOid IS NULL OR r.oid = :routeOid)
              AND (:areaOid IS NULL OR ar.oid = :areaOid)
              AND (:employeeOid IS NULL OR e.oid = :employeeOid)
              AND (:fromDate IS NULL OR d.dcr_date >= :fromDate)
              AND (:toDate IS NULL OR d.dcr_date <= :toDate)
            ORDER BY d.dcr_date DESC, e.emp_name ASC
            """, nativeQuery = true)
    List<DcrReportRow> report(@Param("divisionOid") Long divisionOid,
                               @Param("routeOid") Long routeOid,
                               @Param("areaOid") Long areaOid,
                               @Param("employeeOid") Long employeeOid,
                               @Param("fromDate") LocalDate fromDate,
                               @Param("toDate") LocalDate toDate);
}
