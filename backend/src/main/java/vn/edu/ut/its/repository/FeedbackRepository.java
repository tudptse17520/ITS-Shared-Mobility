package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.entity.Feedback;

/**
 * Repository interface for Feedback entity.
 */
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.ut.its.constant.FeedbackStatus;
import vn.edu.ut.its.constant.IssueType;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT f FROM Feedback f WHERE " +
           "(:issueType IS NULL OR f.issueType = :issueType) AND " +
           "(:status IS NULL OR f.status = :status)")
    Page<Feedback> findAllByFilters(@Param("issueType") IssueType issueType,
                                    @Param("status") FeedbackStatus status,
                                    Pageable pageable);
}
