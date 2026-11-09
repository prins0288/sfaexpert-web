package in.opt.sfa.tenant.dcr.view.repository;

import in.opt.sfa.tenant.dcr.view.entity.DcrReportMarker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Report over the new DCR tables: one row per call, listed clients (dcr_client_summary ->
 * client_master) and unlisted ones (dcr_unlisted_client_summary) together.
 */
public interface DcrRepository extends JpaRepository<DcrReportMarker, Long> {

    @Query(value = """
            SELECT x.oid AS oid, x.empId AS empId, x.dcrDate AS dcrDate, x.workType AS workType, x.remarks AS remarks,
                   x.timeIn AS timeIn, x.timeOut AS timeOut, x.empName AS empName, x.clientName AS clientName,
                   x.clientTypeName AS clientTypeName, x.areaName AS areaName, x.divisionName AS divisionName,
                   x.callKind AS callKind, x.pobValue AS pobValue, x.dcrStatus AS dcrStatus
            FROM (
              SELECT cs.id AS oid, p.emp_id AS empId, p.dcr_date AS dcrDate, p.work_type AS workType, cs.remarks AS remarks,
                     cs.in_time AS timeIn, cs.out_time AS timeOut, e.emp_name AS empName, c.client_name AS clientName,
                     ct.type_name AS clientTypeName, ar.area_name AS areaName, dv.division_name AS divisionName,
                     'Listed' AS callKind, cs.pob_value AS pobValue, p.status AS dcrStatus, e.division_id AS divisionId, cs.area_id AS areaId
              FROM dcr_client_summary cs
              JOIN dcr_parent_summary p ON p.dcr_id = cs.dcr_id
              JOIN emp_detail e ON e.emp_id = p.emp_id
              JOIN client_master c ON c.id = cs.client_id
              LEFT JOIN client_type ct ON ct.oid = c.client_type_id
              LEFT JOIN area_master ar ON ar.oid = COALESCE(cs.area_id, c.city_id)
              LEFT JOIN division_master dv ON dv.oid = e.division_id
              UNION ALL
              SELECT us.id, p.emp_id, p.dcr_date, p.work_type, us.remarks, us.in_time, us.out_time, e.emp_name, us.client_name,
                     ct.type_name, ar.area_name, dv.division_name, 'Unlisted', us.pob_value, p.status, e.division_id, us.area_id
              FROM dcr_unlisted_client_summary us
              JOIN dcr_parent_summary p ON p.dcr_id = us.dcr_id
              JOIN emp_detail e ON e.emp_id = p.emp_id
              LEFT JOIN client_type ct ON ct.oid = us.client_type_id
              LEFT JOIN area_master ar ON ar.oid = us.area_id
              LEFT JOIN division_master dv ON dv.oid = e.division_id
            ) x
            WHERE (:divisionOid IS NULL OR x.divisionId = :divisionOid)
              AND (:areaOid IS NULL OR x.areaId = :areaOid)
              AND (:empId IS NULL OR x.empId = :empId)
              AND (:fromDate IS NULL OR x.dcrDate >= :fromDate)
              AND (:toDate IS NULL OR x.dcrDate <= :toDate)
            ORDER BY x.dcrDate DESC, x.empName ASC, x.timeIn ASC
            LIMIT 5000
            """, nativeQuery = true)
    List<DcrReportRow> report(@Param("divisionOid") Long divisionOid,
                               @Param("areaOid") Long areaOid,
                               @Param("empId") String empId,
                               @Param("fromDate") LocalDate fromDate,
                               @Param("toDate") LocalDate toDate);
}
