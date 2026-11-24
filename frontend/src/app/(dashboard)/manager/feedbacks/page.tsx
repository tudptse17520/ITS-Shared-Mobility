"use client";

import { useState, useMemo } from "react";
import { PlusIcon as Plus } from "@phosphor-icons/react/ssr";
import { Button } from "@/components/ui/button";
import { PageContainer } from "@/components/common/page-container";
import { PageHeader } from "@/components/common/page-header";
import { Pagination } from "@/components/common/pagination";
import { ConfirmDialog } from "@/components/common/confirm-dialog";

import { Feedback, IssueType, FeedbackStatus } from "@/features/feedbacks/types/feedback.type";
import { useFeedbacks } from "@/features/feedbacks/hooks/use-feedbacks";
import { useFeedbackActions } from "@/features/feedbacks/hooks/use-feedback-actions";
import { FeedbackTable } from "@/features/feedbacks/components/feedback-table";
import { FeedbackFilter } from "@/features/feedbacks/components/feedback-filter";
import { FeedbackFormDialog } from "@/features/feedbacks/components/feedback-form-dialog";

export default function FeedbacksPage() {
  const [page, setPage] = useState(1);
  const size = 10;
  const [issueType, setIssueType] = useState<IssueType | undefined>();
  const [status, setStatus] = useState<FeedbackStatus | undefined>();
  const [searchQuery, setSearchQuery] = useState("");

  const [selectedFeedback, setSelectedFeedback] = useState<Feedback | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isReadOnly, setIsReadOnly] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Feedback | null>(null);

  const { data: response, isLoading } = useFeedbacks(page, size, issueType, status);
  const { deleteFeedbackMutation } = useFeedbackActions();

  const handleIssueTypeChange = (value: IssueType | "ALL") => {
    setIssueType(value === "ALL" ? undefined : value);
    setPage(1);
  };

  const handleStatusChange = (value: FeedbackStatus | "ALL") => {
    setStatus(value === "ALL" ? undefined : value);
    setPage(1);
  };

  const handleCreate = () => {
    setSelectedFeedback(null);
    setIsReadOnly(false);
    setIsFormOpen(true);
  };

  const handleView = (feedback: Feedback) => {
    setSelectedFeedback(feedback);
    setIsReadOnly(true);
    setIsFormOpen(true);
  };

  const handleEdit = (feedback: Feedback) => {
    setSelectedFeedback(feedback);
    setIsReadOnly(false);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setSelectedFeedback(null);
    setIsReadOnly(false);
  };

  const handleOpenDelete = (feedback: Feedback) => {
    setDeleteTarget(feedback);
  };

  const handleConfirmDelete = () => {
    if (deleteTarget?.id) {
      deleteFeedbackMutation.mutate(deleteTarget.id, {
        onSuccess: () => {
          setDeleteTarget(null);
        },
      });
    }
  };

  const displayData = useMemo(() => {
    let data = response?.data || [];
    if (searchQuery) {
      const lowerQuery = searchQuery.toLowerCase();
      data = data.filter((item: any) => {
        const mockLicensePlate = `51f-${100 + (item.id % 900)}.${10 + (item.id % 90)}`.toLowerCase();
        return (
          String(item.id).toLowerCase().includes(lowerQuery) ||
          String(item.rideSessionId).toLowerCase().includes(lowerQuery) ||
          mockLicensePlate.includes(lowerQuery)
        );
      });
    }
    return data;
  }, [response?.data, searchQuery]);

  return (
    <PageContainer>
      <PageHeader
        title="Quản lý Sự cố"
        description="Theo dõi và giải quyết các sự cố phát sinh tại bãi đỗ xe (mất thẻ, sai biển số, v.v.)"
        actions={
          <Button onClick={handleCreate}>
            <Plus className="mr-2 h-4 w-4" />
            Ghi nhận sự cố
          </Button>
        }
      />

      <FeedbackFilter
        onSearchChange={setSearchQuery}
        onIssueTypeChange={handleIssueTypeChange}
        onStatusChange={handleStatusChange}
      />

      <div className="bg-card rounded-lg shadow mt-6">
        <FeedbackTable
          data={displayData}
          isLoading={isLoading}
          onView={handleView}
          onEdit={handleEdit}
          onDelete={handleOpenDelete}
        />

        {response && response.totalPages > 1 && (
          <div className="p-4 border-t">
            <Pagination
              currentPage={page}
              totalPages={response.totalPages}
              onPageChange={setPage}
            />
          </div>
        )}
      </div>

      <FeedbackFormDialog
        isOpen={isFormOpen}
        onClose={handleCloseForm}
        feedback={selectedFeedback}
        readOnly={isReadOnly}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa sự cố"
        description={`Bạn có chắc chắn muốn xóa sự cố ${deleteTarget ? "#" + deleteTarget.id : ""} không? Hành động này không thể hoàn tác.`}
        confirmText="Xóa sự cố"
        cancelText="Đóng"
        variant="danger"
        isLoading={deleteFeedbackMutation.isPending}
      />
    </PageContainer>
  );
}
