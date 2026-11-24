-- Run on an existing ITS database while the application is stopped.
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.ride_session', N'U') IS NULL
        OR OBJECT_ID(N'dbo.booking', N'U') IS NULL
        OR OBJECT_ID(N'dbo.feedback', N'U') IS NULL
        OR OBJECT_ID(N'dbo.payment', N'U') IS NULL
        THROW 50001, 'Select an existing ITS database before running the migration.', 1;

    DECLARE @renames TABLE (table_name sysname, old_name sysname, new_name sysname);
    INSERT INTO @renames VALUES
        (N'ride_session', N'ticket_code', N'session_code'),
        (N'ride_session', N'time_in', N'started_at'),
        (N'ride_session', N'time_out', N'ended_at'),
        (N'booking', N'expected_time_in', N'planned_start_at'),
        (N'booking', N'expected_time_out', N'planned_end_at');

    DECLARE @table sysname, @old sysname, @new sysname, @columnPath nvarchar(776);
    DECLARE rename_columns CURSOR LOCAL FAST_FORWARD FOR
        SELECT table_name, old_name, new_name FROM @renames;
    OPEN rename_columns;
    FETCH NEXT FROM rename_columns INTO @table, @old, @new;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        IF COL_LENGTH(N'dbo.' + @table, @old) IS NOT NULL
        BEGIN
            IF COL_LENGTH(N'dbo.' + @table, @new) IS NOT NULL
                THROW 50002, 'Conflicting columns exist. Review the schema before continuing.', 1;
            SET @columnPath = QUOTENAME(N'dbo') + N'.' + QUOTENAME(@table) + N'.' + QUOTENAME(@old);
            EXEC sys.sp_rename @objname = @columnPath, @newname = @new, @objtype = N'COLUMN';
        END
        ELSE IF COL_LENGTH(N'dbo.' + @table, @new) IS NULL
            THROW 50003, 'The existing schema is missing an expected column.', 1;
        FETCH NEXT FROM rename_columns INTO @table, @old, @new;
    END
    CLOSE rename_columns;
    DEALLOCATE rename_columns;

    -- SQL Server may have generated check constraints on the string enums.
    DECLARE @enums TABLE (
        table_name sysname, column_name sysname, constraint_name sysname,
        legacy_marker nvarchar(50), update_sql nvarchar(max), allowed_values nvarchar(300)
    );
    INSERT INTO @enums VALUES
        (N'booking', N'status', N'CK_booking_status_its', N'CHECKED_IN',
         N'UPDATE dbo.booking SET [status] = ''USED'' WHERE [status] = ''CHECKED_IN'';',
         N'''PENDING'', ''CONFIRMED'', ''USED'', ''CANCELLED'', ''EXPIRED'''),
        (N'feedback', N'issue_type', N'CK_feedback_issue_type_its', N'LOST_TICKET',
         N'UPDATE dbo.feedback SET issue_type = CASE issue_type
              WHEN ''LOST_TICKET'' THEN ''OTHER''
              WHEN ''WRONG_PLACE'' THEN ''WRONG_RETURN_LOCATION''
              WHEN ''OVERTIME'' THEN ''RIDE_OVERTIME''
              WHEN ''UNPAID_EXIT'' THEN ''UNPAID_RIDE''
            END WHERE issue_type IN (''LOST_TICKET'', ''WRONG_PLACE'', ''OVERTIME'', ''UNPAID_EXIT'');',
         N'''OTHER'', ''WRONG_PLATE'', ''RIDE_OVERTIME'', ''WRONG_RETURN_LOCATION'', ''UNPAID_RIDE'''),
        (N'payment', N'fee_type', N'CK_payment_fee_type_its', N'LOST_TICKET_FINE',
         N'UPDATE dbo.payment SET fee_type = ''HISTORICAL_ADJUSTMENT'' WHERE fee_type = ''LOST_TICKET_FINE'';',
         N'''RIDE_FEE'', ''BOOKING_DEPOSIT'', ''HISTORICAL_ADJUSTMENT''');

    DECLARE @column sysname, @constraint sysname, @marker nvarchar(50),
        @update nvarchar(max), @allowed nvarchar(300), @sql nvarchar(max), @tablePath nvarchar(300);
    DECLARE update_enums CURSOR LOCAL FAST_FORWARD FOR
        SELECT table_name, column_name, constraint_name, legacy_marker, update_sql, allowed_values FROM @enums;
    OPEN update_enums;
    FETCH NEXT FROM update_enums INTO @table, @column, @constraint, @marker, @update, @allowed;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @tablePath = N'[dbo].' + QUOTENAME(@table);
        SET @sql = N'';
        SELECT @sql += N'ALTER TABLE ' + @tablePath + N' DROP CONSTRAINT ' + QUOTENAME(name) + N';'
        FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(@tablePath)
          AND CHARINDEX(QUOTENAME(@column), definition) > 0
          AND CHARINDEX(N'''' + @marker + N'''', definition) > 0;
        IF @sql <> N'' EXEC sys.sp_executesql @sql;

        EXEC sys.sp_executesql @update;
        IF OBJECT_ID(N'dbo.' + @constraint, N'C') IS NULL
        BEGIN
            SET @sql = N'ALTER TABLE ' + @tablePath + N' WITH CHECK ADD CONSTRAINT '
                + QUOTENAME(@constraint) + N' CHECK (' + QUOTENAME(@column) + N' IN (' + @allowed + N'));';
            EXEC sys.sp_executesql @sql;
        END
        FETCH NEXT FROM update_enums INTO @table, @column, @constraint, @marker, @update, @allowed;
    END
    CLOSE update_enums;
    DEALLOCATE update_enums;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
