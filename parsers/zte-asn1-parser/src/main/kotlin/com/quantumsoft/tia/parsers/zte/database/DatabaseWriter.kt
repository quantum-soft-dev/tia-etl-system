package com.quantumsoft.tia.parsers.zte.database

import com.quantumsoft.tia.parsers.zte.core.model.InsertResult
import com.quantumsoft.tia.parsers.zte.core.model.SchemaValidationResult
import com.quantumsoft.tia.parsers.zte.core.model.ZteCdrRecord

/**
 * Interface for ClickHouse database operations following Single Responsibility Principle.
 * 
 * This interface focuses exclusively on database insert operations and schema
 * validation, providing a clean abstraction over ClickHouse-specific operations.
 * It is designed to support high-performance batch insertions while maintaining
 * data integrity through comprehensive schema validation.
 * 
 * ## Design Principles
 * 
 * - **Single Responsibility**: Only handles database write operations
 * - **Interface Segregation**: Minimal interface focused on essential operations
 * - **Open/Closed**: Extensible for different database backends without modification
 * - **Liskov Substitution**: All implementations fully interchangeable
 * - **Dependency Inversion**: Database-agnostic interface with specific implementations
 * 
 * ## ClickHouse Optimization Features
 * 
 * - Batch insertion for optimal performance
 * - Non-nullable schema enforcement
 * - Connection pool management
 * - Transaction handling for consistency
 * - Prepared statement optimization
 * 
 * ## Performance Requirements
 * 
 * - Process &gt;100,000 records per minute in batch mode
 * - Memory usage &lt;512MB for batch operations
 * - Connection pool efficiency for concurrent operations
 * - Transaction rollback support for error recovery
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
interface DatabaseWriter {
    
    /**
     * Inserts a batch of validated ZTE CDR records into ClickHouse.
     * 
     * This method performs optimized batch insertion of validated records into
     * the target ClickHouse table. It handles transaction management, connection
     * pooling, and error recovery while maintaining data consistency and performance.
     * 
     * ## Batch Processing Flow
     * 
     * 1. Validate records against ClickHouse schema
     * 2. Prepare batch insert statement with optimal parameters
     * 3. Execute batch insertion within transaction boundary
     * 4. Handle partial failures and rollback scenarios
     * 5. Update processing statistics and metrics
     * 6. Clean up resources and return detailed results
     * 
     * ## Transaction Management
     * 
     * - Uses database transactions to ensure atomicity
     * - Supports rollback on validation or insertion failures
     * - Manages connection lifecycle properly
     * - Handles timeout and retry scenarios
     * 
     * ## Performance Optimizations
     * 
     * - Prepared statement reuse for batch operations
     * - Connection pool optimization for concurrent access
     * - Optimal batch sizing based on memory and performance
     * - Asynchronous processing where appropriate
     * 
     * ## Error Handling
     * 
     * - Individual record error isolation
     * - Detailed error reporting for troubleshooting
     * - Partial success handling for large batches
     * - Connection failure recovery and retry logic
     * 
     * @param records List of validated ZTE CDR records to insert
     * @return InsertResult with detailed success/failure information and performance metrics
     * 
     * @see InsertResult for result structure and error details
     * @see ZteCdrRecord for record structure requirements
     */
    suspend fun batchInsert(records: List&lt;ZteCdrRecord&gt;): InsertResult
    
    /**
     * Validates a record against the target ClickHouse schema before insertion.
     * 
     * This method performs comprehensive schema validation to ensure that all
     * record fields are compatible with the target ClickHouse table structure.
     * It verifies data types, constraints, and the non-nullable requirement
     * of the optimized schema design.
     * 
     * ## Schema Validation Checks
     * 
     * - **Type Compatibility**: Ensure field types match ClickHouse column types
     * - **Null Constraints**: Verify no fields are null (non-nullable schema)
     * - **Length Constraints**: Check string field length limits
     * - **Range Constraints**: Validate numeric field ranges
     * - **Format Constraints**: Verify date/time and enum field formats
     * 
     * ## Non-Nullable Schema Design
     * 
     * The target ClickHouse schema uses no nullable fields to optimize storage
     * and query performance. All fields must have appropriate default values
     * or be populated with valid data. This method enforces these constraints
     * before insertion attempts.
     * 
     * ## Performance Considerations
     * 
     * - Schema metadata caching for repeated validations
     * - Fast validation path for common scenarios
     * - Minimal overhead for high-volume processing
     * - Early failure detection to avoid expensive operations
     * 
     * @param record ZTE CDR record to validate against schema
     * @return SchemaValidationResult indicating compliance or specific constraint violations
     * 
     * @see SchemaValidationResult for validation outcome details
     * @see ZteCdrRecord for record structure definition
     */
    fun validateSchema(record: ZteCdrRecord): SchemaValidationResult
    
    /**
     * Validates an entire batch of records against schema constraints efficiently.
     * 
     * This method provides optimized schema validation for multiple records,
     * leveraging shared schema metadata and validation logic to improve performance
     * while maintaining individual record validation results.
     * 
     * ## Batch Validation Optimizations
     * 
     * - Shared schema metadata loading
     * - Parallel validation where thread-safe
     * - Early batch termination for critical failures
     * - Optimized constraint checking for similar values
     * 
     * ## Result Aggregation
     * 
     * Returns individual validation results for each record plus aggregate
     * statistics about batch validation performance and outcomes.
     * 
     * @param records List of ZTE CDR records to validate
     * @return List of SchemaValidationResult corresponding to input records
     */
    fun validateSchemaBatch(records: List&lt;ZteCdrRecord&gt;): List&lt;SchemaValidationResult&gt;
    
    /**
     * Retrieves current ClickHouse connection and schema health status.
     * 
     * This method performs health checks on the database connection and
     * verifies that the target schema is accessible and properly configured.
     * It can be used for monitoring and initialization verification.
     * 
     * ## Health Check Components
     * 
     * - Database connection availability
     * - Target table existence and accessibility
     * - Schema structure correctness
     * - Connection pool health status
     * - Transaction support verification
     * 
     * @return true if database connection and schema are healthy and operational
     */
    fun isDatabaseHealthy(): Boolean
    
    /**
     * Prepares the database writer for shutdown and resource cleanup.
     * 
     * This method handles graceful shutdown of database connections, connection
     * pools, and other resources managed by the database writer. It ensures
     * that all pending operations complete and resources are properly released.
     * 
     * ## Cleanup Operations
     * 
     * - Complete pending batch operations
     * - Close database connections properly  
     * - Release connection pool resources
     * - Clean up prepared statements
     * - Flush any cached data
     */
    suspend fun cleanup()
}