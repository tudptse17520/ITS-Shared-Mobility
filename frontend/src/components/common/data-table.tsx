// ---------------------------------------------
// Data Table
// Bọc sẵn TanStack Table tích hợp phân trang, bộ lọc dữ liệu nhanh
// ---------------------------------------------

"use client";

import {
  useReactTable,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  flexRender,
  type ColumnDef,
  type SortingState,
  type ColumnFiltersState,
} from "@tanstack/react-table";
import { useState } from "react";
import { cn } from "@/lib/utils";
import { ArrowsDownUpIcon as ArrowUpDown, ArrowUpIcon as ArrowUp, ArrowDownIcon as ArrowDown, CaretLeftIcon as ChevronLeft, CaretRightIcon as ChevronRight } from "@phosphor-icons/react/ssr";
import { EmptyState } from "@/components/common/empty-state";
import { Skeleton } from "@/components/ui/skeleton";

interface DataTableProps<TData, TValue> {
  columns: ColumnDef<TData, TValue>[];
  data: TData[];
  isLoading?: boolean;
  searchKey?: string;
  searchValue?: string;
  pageSize?: number;
  className?: string;
  pageSizeOptions?: number[];
}

export function DataTable<TData, TValue>({
  columns,
  data,
  isLoading = false,
  pageSize = 10,
  pageSizeOptions = [10, 20, 50, 100],
  className,
}: DataTableProps<TData, TValue>) {
  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);

  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    state: {
      sorting,
      columnFilters,
    },
    initialState: {
      pagination: {
        pageSize,
      },
    },
  });

  if (isLoading) {
    return (
      <div className={cn("space-y-4", className)}>
        <div className="table-viewport rounded-[18px] border bg-card">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border/50 bg-muted/40">
                {columns.map((_, i) => (
                  <th key={i} className="px-4 py-3.5"><Skeleton className="h-4 w-20" /></th>
                ))}
              </tr>
            </thead>
            <tbody>
              {Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="border-b border-border/50">
                  {columns.map((_, j) => (
                    <td key={j} className="px-4 py-3"><Skeleton className="h-4 w-full" /></td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  return (
    <div className={cn("space-y-4", className)}>
      {/* Table */}
      <div className="table-viewport rounded-[18px] border bg-card">
        <table className="w-full">
          <thead>
            {table.getHeaderGroups().map((headerGroup) => (
              <tr
                key={headerGroup.id}
                className="border-b bg-muted/70 text-foreground"
              >
                {headerGroup.headers.map((header) => (
                  <th
                    key={header.id}
                    className="group px-4 py-3.5 text-left text-xs font-semibold text-muted-foreground"
                  >
                    {header.isPlaceholder ? null : (
                      <div
                        className={cn(
                          "flex items-center gap-1.5",
                          header.column.getCanSort() &&
                            "cursor-pointer select-none hover:text-foreground transition-colors"
                        )}
                        onClick={header.column.getToggleSortingHandler()}
                      >
                        {flexRender(
                          header.column.columnDef.header,
                          header.getContext()
                        )}
                        {header.column.getCanSort() && (
                          <span className="flex items-center">
                            {header.column.getIsSorted() === "asc" ? (
                              <ArrowUp className="h-3.5 w-3.5 text-primary" />
                            ) : header.column.getIsSorted() === "desc" ? (
                              <ArrowDown className="h-3.5 w-3.5 text-primary" />
                            ) : (
                              <ArrowUpDown className="h-3.5 w-3.5 text-muted-foreground/40 opacity-0 group-hover:opacity-100 transition-opacity" />
                            )}
                          </span>
                        )}
                      </div>
                    )}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody>
            {table.getRowModel().rows.length ? (
              table.getRowModel().rows.map((row) => (
                  <tr
                  key={row.id}
                  className="border-b transition-colors duration-150 hover:bg-muted/55 last:border-0"
                >
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className="px-4 py-3 text-sm align-middle h-13">
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </td>
                  ))}
                </tr>
              ))
            ) : (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-12"
                >
                  <EmptyState 
                    title="Không có dữ liệu"
                    description="Danh sách trống hoặc không có kết quả phù hợp."
                  />
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="flex flex-wrap items-center justify-between gap-3 px-1 py-1 text-sm text-muted-foreground">
        <div className="flex items-center gap-2">
          <span className="text-xs sm:text-sm font-medium">Hiển thị</span>
          <select
            value={table.getState().pagination.pageSize}
            aria-label="Số dòng mỗi trang"
            onChange={(e) => table.setPageSize(Number(e.target.value))}
            className="h-8 rounded-md border border-border/60 bg-background px-2 py-1 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
          >
            {pageSizeOptions.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
          <span className="text-xs sm:text-sm font-medium">dòng</span>
        </div>
        
        <div className="flex items-center gap-4">
          <span className="text-xs sm:text-sm">
            Trang <span className="font-semibold text-foreground">{table.getPageCount() > 0 ? table.getState().pagination.pageIndex + 1 : 0}</span> / {table.getPageCount()}
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="inline-flex items-center justify-center h-8 w-8 rounded-lg border border-border/60 bg-background hover:bg-muted disabled:opacity-40 transition-colors duration-200"
              aria-label="Trang trước"
              onClick={() => table.previousPage()}
              disabled={!table.getCanPreviousPage() || isLoading}
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              type="button"
              className="inline-flex items-center justify-center h-8 w-8 rounded-lg border border-border/60 bg-background hover:bg-muted disabled:opacity-40 transition-colors duration-200"
              aria-label="Trang sau"
              onClick={() => table.nextPage()}
              disabled={!table.getCanNextPage() || isLoading}
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
