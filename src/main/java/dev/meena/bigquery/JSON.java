/*
 * BigQuery API
 * A data platform for customers to create, manage, share and query data.
 *
 * The version of the OpenAPI document: v2
 */
package dev.meena.bigquery;

import com.google.gson.*;
import com.google.gson.internal.bind.util.ISO8601Utils;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dev.meena.bigquery.fn.ByteString;
import dev.meena.bigquery.model.*;
import io.gsonfire.GsonFireBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/*
 * A JSON utility class
 *
 * NOTE: in the future, this class may be converted to static, which may break
 *       backward-compatibility
 */
public class JSON {

    private static Gson gson;
    private static boolean isLenientOnJson = false;
    private static final DateTypeAdapter dateTypeAdapter = new DateTypeAdapter();
    private static final SqlDateTypeAdapter sqlDateTypeAdapter = new SqlDateTypeAdapter();
    private static final OffsetDateTimeTypeAdapter offsetDateTimeTypeAdapter = new OffsetDateTimeTypeAdapter();
    private static final LocalDateTypeAdapter localDateTypeAdapter = new LocalDateTypeAdapter();
    private static final ByteArrayAdapter byteArrayAdapter = new ByteArrayAdapter();

    public static GsonBuilder createGson() {
        GsonFireBuilder fireBuilder = new GsonFireBuilder();
        return fireBuilder.createGsonBuilder();
    }

    static {
        GsonBuilder gsonBuilder = createGson();
        gsonBuilder.registerTypeAdapter(Date.class, dateTypeAdapter);
        gsonBuilder.registerTypeAdapter(java.sql.Date.class, sqlDateTypeAdapter);
        gsonBuilder.registerTypeAdapter(OffsetDateTime.class, offsetDateTimeTypeAdapter);
        gsonBuilder.registerTypeAdapter(LocalDate.class, localDateTypeAdapter);
        gsonBuilder.registerTypeAdapter(byte[].class, byteArrayAdapter);
        gsonBuilder.registerTypeAdapterFactory(new AggregateClassificationMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new AggregationThresholdPolicy.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Argument.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ArimaCoefficients.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ArimaFittingMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ArimaForecastingMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ArimaModelInfo.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ArimaOrder.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ArimaResult.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ArimaSingleModelForecastingMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new AuditConfig.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new AuditLogConfig.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new AvroOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BiEngineReason.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BiEngineStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BigLakeConfiguration.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BigQueryModelTraining.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BigtableColumn.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BigtableColumnFamily.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BigtableOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BinaryClassificationMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BinaryConfusionMatrix.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Binding.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BqmlIterationResult.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BqmlTrainingRun.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new BqmlTrainingRunTrainingOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new CategoricalValue.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new CategoryCount.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new CloneDefinition.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Cluster.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ClusterInfo.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Clustering.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ClusteringMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ConfusionMatrix.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ConnectionProperty.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new CsvOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DataFormatOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DataMaskingStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DataSplitResult.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Dataset.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DatasetAccessEntry.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DatasetAccessInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DatasetList.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DatasetListDatasetsInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DatasetReference.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DatasetTagsInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DestinationTableProperties.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DimensionalityReductionMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DmlStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DoubleCandidates.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DoubleHparamSearchSpace.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new DoubleRange.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new EncryptionConfiguration.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Entry.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ErrorProto.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new EvaluationMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ExplainQueryStage.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ExplainQueryStep.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Explanation.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ExportDataStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Expr.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ExternalDataConfiguration.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ExternalDatasetReference.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ExternalServiceCost.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new FeatureValue.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new GetIamPolicyRequest.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new GetPolicyOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new GetQueryResultsResponse.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new GetServiceAccountResponse.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new GlobalExplanation.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new GoogleSheetsOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new HighCardinalityJoin.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new HivePartitioningOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new HparamSearchSpaces.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new HparamTuningTrial.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new IndexUnusedReason.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new InputDataChange.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new IntArray.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new IntArrayHparamSearchSpace.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new IntCandidates.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new IntHparamSearchSpace.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new IntRange.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new IterationResult.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Job.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobCancelResponse.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobConfiguration.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobConfigurationExtract.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobConfigurationLoad.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobConfigurationQuery.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobConfigurationTableCopy.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobCreationReason.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobList.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobListJobsInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobReference.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobStatistics2.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobStatistics3.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobStatistics4.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobStatistics5.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobStatisticsReservationUsageInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JobStatus.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new JsonOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new LinkedDatasetSource.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ListModelsResponse.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ListRoutinesResponse.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ListRowAccessPoliciesResponse.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new LoadQueryStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new LocationMetadata.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new MaterializedView.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new MaterializedViewDefinition.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new MaterializedViewStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new MaterializedViewStatus.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new MetadataCacheStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new MlStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Model.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ModelDefinition.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ModelDefinitionModelOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ModelExtractOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ModelReference.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new MultiClassClassificationMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ParquetOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new PerformanceInsights.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Policy.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new PrincipalComponentInfo.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new PrivacyPolicy.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ProjectList.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ProjectListProjectsInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ProjectReference.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new QueryInfo.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new QueryParameter.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new QueryParameterType.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new QueryParameterTypeStructTypesInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new QueryParameterValue.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new QueryRequest.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new QueryResponse.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new QueryTimelineSample.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RangePartitioning.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RangePartitioningRange.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RangeValue.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RankingMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RegressionMetrics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RemoteFunctionOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RemoteModelInfo.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Routine.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RoutineReference.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Row.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RowAccessPolicy.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RowAccessPolicyReference.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new RowLevelSecurityStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ScriptOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ScriptStackFrame.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ScriptStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new SearchStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new SessionInfo.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new SetIamPolicyRequest.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new SnapshotDefinition.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new SparkLoggingInfo.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new SparkOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new SparkStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new StagePerformanceChangeInsight.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new StagePerformanceStandaloneInsight.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new StandardSqlDataType.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new StandardSqlField.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new StandardSqlStructType.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new StandardSqlTableType.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Streamingbuffer.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new StringHparamSearchSpace.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new SystemVariables.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new Table.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableCell.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableConstraints.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableConstraintsForeignKeysInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableConstraintsForeignKeysInnerColumnReferencesInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableConstraintsForeignKeysInnerReferencedTable.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableConstraintsPrimaryKey.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableDataInsertAllRequest.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableDataInsertAllRequestRowsInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableDataInsertAllResponse.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableDataInsertAllResponseInsertErrorsInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableDataList.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableFieldSchema.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableFieldSchemaCategories.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableFieldSchemaPolicyTags.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableFieldSchemaRangeElementType.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableList.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableListTablesInner.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableListTablesInnerView.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableMetadataCacheUsage.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableReference.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableReplicationInfo.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableRow.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TableSchema.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TestIamPermissionsRequest.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TestIamPermissionsResponse.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TimePartitioning.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TrainingOptions.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TrainingRun.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TransactionInfo.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new TransformColumn.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new UndeleteDatasetRequest.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new UserDefinedFunctionResource.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new VectorSearchStatistics.CustomTypeAdapterFactory());
        gsonBuilder.registerTypeAdapterFactory(new ViewDefinition.CustomTypeAdapterFactory());
        gson = gsonBuilder.create();
    }

    /**
     * Get Gson.
     *
     * @return Gson
     */
    public static Gson getGson() {
        return gson;
    }

    /**
     * Set Gson.
     *
     * @param gson Gson
     */
    public static void setGson(Gson gson) {
        JSON.gson = gson;
    }

    public static void setLenientOnJson(boolean lenientOnJson) {
        isLenientOnJson = lenientOnJson;
    }

    /**
     * Serialize the given Java object into a JSON string.
     *
     * @param obj Object
     * @return String representation of the JSON
     */
    public static String serialize(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * Deserialize the given JSON string to a Java object.
     *
     * @param <T>        Type
     * @param body       The JSON string
     * @param returnType The type to deserialize into
     * @return The deserialized Java object
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(String body, Type returnType) {
        try {
            if (isLenientOnJson) {
                JsonReader jsonReader = new JsonReader(new StringReader(body));
                // see https://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/stream/JsonReader.html#setLenient(boolean)
                jsonReader.setStrictness(Strictness.LENIENT);
                return gson.fromJson(jsonReader, returnType);
            } else {
                return gson.fromJson(body, returnType);
            }
        } catch (JsonParseException e) {
            // Fallback processing when failed to parse JSON from response body:
            // return the response body string directly for the String return type;
            if (returnType.equals(String.class)) {
                return (T) body;
            } else {
                throw (e);
            }
        }
    }

    /**
     * Deserialize the given JSON InputStream to a Java object.
     *
     * @param <T>         Type
     * @param inputStream The JSON InputStream
     * @param returnType  The type to deserialize into
     * @return The deserialized Java object
     */
    public static <T> T deserialize(InputStream inputStream, Type returnType) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            if (isLenientOnJson) {
                // see https://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/stream/JsonReader.html#setLenient(boolean)
                JsonReader jsonReader = new JsonReader(reader);
                jsonReader.setStrictness(Strictness.LENIENT);
                return gson.fromJson(jsonReader, returnType);
            } else {
                return gson.fromJson(reader, returnType);
            }
        }
    }

    /**
     * Gson TypeAdapter for Byte Array type
     */
    public static class ByteArrayAdapter extends TypeAdapter<byte[]> {

        @Override
        public void write(JsonWriter out, byte[] value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(ByteString.of(value).base64());
            }
        }

        @Override
        public byte[] read(JsonReader in) throws IOException {
            if (Objects.requireNonNull(in.peek()) == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String bytesAsBase64 = in.nextString();
            ByteString byteString = ByteString.decodeBase64(bytesAsBase64);
            return byteString != null ? byteString.toByteArray() : null;
        }
    }

    /**
     * Gson TypeAdapter for JSR310 OffsetDateTime type
     */
    public static class OffsetDateTimeTypeAdapter extends TypeAdapter<OffsetDateTime> {

        private DateTimeFormatter formatter;

        public OffsetDateTimeTypeAdapter() {
            this(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }

        public OffsetDateTimeTypeAdapter(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        public void setFormat(DateTimeFormatter dateFormat) {
            this.formatter = dateFormat;
        }

        @Override
        public void write(JsonWriter out, OffsetDateTime date) throws IOException {
            if (date == null) {
                out.nullValue();
            } else {
                out.value(formatter.format(date));
            }
        }

        @Override
        public OffsetDateTime read(JsonReader in) throws IOException {
            if (Objects.requireNonNull(in.peek()) == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String date = in.nextString();
            if (date.endsWith("+0000")) {
                date = date.substring(0, date.length() - 5) + "Z";
            }
            return OffsetDateTime.parse(date, formatter);
        }
    }

    /**
     * Gson TypeAdapter for JSR310 LocalDate type
     */
    public static class LocalDateTypeAdapter extends TypeAdapter<LocalDate> {

        private DateTimeFormatter formatter;

        public LocalDateTypeAdapter() {
            this(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        public LocalDateTypeAdapter(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        public void setFormat(DateTimeFormatter dateFormat) {
            this.formatter = dateFormat;
        }

        @Override
        public void write(JsonWriter out, LocalDate date) throws IOException {
            if (date == null) {
                out.nullValue();
            } else {
                out.value(formatter.format(date));
            }
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (Objects.requireNonNull(in.peek()) == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String date = in.nextString();
            return LocalDate.parse(date, formatter);
        }
    }

    public static void setOffsetDateTimeFormat(DateTimeFormatter dateFormat) {
        offsetDateTimeTypeAdapter.setFormat(dateFormat);
    }

    public static void setLocalDateFormat(DateTimeFormatter dateFormat) {
        localDateTypeAdapter.setFormat(dateFormat);
    }

    /**
     * Gson TypeAdapter for java.sql.Date type
     * If the dateFormat is null, a simple "yyyy-MM-dd" format will be used
     * (more efficient than SimpleDateFormat).
     */
    public static class SqlDateTypeAdapter extends TypeAdapter<java.sql.Date> {

        private DateFormat dateFormat;

        public SqlDateTypeAdapter() {
        }

        public SqlDateTypeAdapter(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        public void setFormat(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        @Override
        public void write(JsonWriter out, java.sql.Date date) throws IOException {
            if (date == null) {
                out.nullValue();
            } else {
                String value;
                if (dateFormat != null) {
                    value = dateFormat.format(date);
                } else {
                    value = date.toString();
                }
                out.value(value);
            }
        }

        @Override
        public java.sql.Date read(JsonReader in) throws IOException {
            if (Objects.requireNonNull(in.peek()) == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String date = in.nextString();
            try {
                if (dateFormat != null) {
                    return new java.sql.Date(dateFormat.parse(date).getTime());
                }
                return new java.sql.Date(ISO8601Utils.parse(date, new ParsePosition(0)).getTime());
            } catch (ParseException e) {
                throw new JsonParseException(e);
            }
        }
    }

    /**
     * Gson TypeAdapter for java.util.Date type
     * If the dateFormat is null, ISO8601Utils will be used.
     */
    public static class DateTypeAdapter extends TypeAdapter<Date> {

        private DateFormat dateFormat;

        public DateTypeAdapter() {
        }

        public DateTypeAdapter(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        public void setFormat(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        @Override
        public void write(JsonWriter out, Date date) throws IOException {
            if (date == null) {
                out.nullValue();
            } else {
                String value;
                if (dateFormat != null) {
                    value = dateFormat.format(date);
                } else {
                    value = ISO8601Utils.format(date, true);
                }
                out.value(value);
            }
        }

        @Override
        public Date read(JsonReader in) throws IOException {
            try {
                if (Objects.requireNonNull(in.peek()) == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                String date = in.nextString();
                try {
                    if (dateFormat != null) {
                        return dateFormat.parse(date);
                    }
                    return ISO8601Utils.parse(date, new ParsePosition(0));
                } catch (ParseException e) {
                    throw new JsonParseException(e);
                }
            } catch (IllegalArgumentException e) {
                throw new JsonParseException(e);
            }
        }
    }

    public static void setDateFormat(DateFormat dateFormat) {
        dateTypeAdapter.setFormat(dateFormat);
    }

    public static void setSqlDateFormat(DateFormat dateFormat) {
        sqlDateTypeAdapter.setFormat(dateFormat);
    }
}
