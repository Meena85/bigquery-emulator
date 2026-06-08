package org.openapitools.api;

import dev.meena.bigquery.model.Dataset;
import dev.meena.bigquery.model.DatasetList;
import dev.meena.bigquery.model.GetQueryResultsResponse;
import dev.meena.bigquery.model.GetServiceAccountResponse;
import dev.meena.bigquery.model.Job;
import dev.meena.bigquery.model.JobCancelResponse;
import dev.meena.bigquery.model.JobList;
import dev.meena.bigquery.model.ListModelsResponse;
import dev.meena.bigquery.model.ListRoutinesResponse;
import dev.meena.bigquery.model.ListRowAccessPoliciesResponse;
import dev.meena.bigquery.model.Model;
import dev.meena.bigquery.model.ProjectList;
import dev.meena.bigquery.model.QueryRequest;
import dev.meena.bigquery.model.QueryResponse;
import dev.meena.bigquery.model.Routine;
import dev.meena.bigquery.model.Table;
import dev.meena.bigquery.model.TableDataInsertAllRequest;
import dev.meena.bigquery.model.TableDataInsertAllResponse;
import dev.meena.bigquery.model.TableDataList;
import dev.meena.bigquery.model.TableList;
import dev.meena.bigquery.model.UndeleteDatasetRequest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import io.swagger.annotations.*;

import java.io.InputStream;
import java.util.Map;
import java.util.List;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

/**
 * Represents a collection of functions to interact with the API endpoints.
 */
@Path("/projects")
@Api(description = "the projects API")
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-06-07T14:28:13.177465-04:00[America/Toronto]", comments = "Generator version: 7.22.0")
public class ProjectsApi {

    @DELETE
    @Path("/{projectId}/datasets/{datasetId}")
    @ApiOperation(value = "", notes = "Deletes the dataset specified by the datasetId value. Before you can delete a dataset, you must delete all its tables, either manually or by specifying deleteContents. Immediately after deletion, you can create another dataset with the same name.", response = Void.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "datasets" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Void.class)
    })
    public Response bigqueryDatasetsDelete(@PathParam("projectId") @ApiParam("Required. Project ID of the dataset being deleted") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of dataset being deleted") String datasetId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("deleteContents")  @ApiParam("If True, delete all the tables in the dataset. If False and the dataset contains tables, the request will fail. Default is False")  Boolean deleteContents) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/datasets/{datasetId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Returns the dataset specified by datasetID.", response = Dataset.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "datasets" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Dataset.class)
    })
    public Response bigqueryDatasetsGet(@PathParam("projectId") @ApiParam("Required. Project ID of the requested dataset") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the requested dataset") String datasetId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("datasetView")  @ApiParam("Optional. Specifies the view that determines which dataset information is returned. By default, metadata and ACL information are returned.")  String datasetView) {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/{projectId}/datasets")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Creates a new empty dataset.", response = Dataset.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "datasets" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Dataset.class)
    })
    public Response bigqueryDatasetsInsert(@PathParam("projectId") @ApiParam("Required. Project ID of the new dataset") String projectId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid Dataset dataset) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/datasets")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Lists all datasets in the specified project to which the user has been granted the READER dataset role.", response = DatasetList.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "datasets" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = DatasetList.class)
    })
    public Response bigqueryDatasetsList(@PathParam("projectId") @ApiParam("Required. Project ID of the datasets to be listed") String projectId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("all")  @ApiParam("Whether to list all datasets, including hidden ones")  Boolean all,@QueryParam("filter")  @ApiParam("An expression for filtering the results of the request by label. The syntax is \\\&quot;labels.&lt;name&gt;[:&lt;value&gt;]\\\&quot;. Multiple filters can be ANDed together by connecting with a space. Example: \\\&quot;labels.department:receiving labels.active\\\&quot;. See [Filtering datasets using labels](/bigquery/docs/labeling-datasets#filtering_datasets_using_labels) for details.")  String filter,@QueryParam("maxResults")  @ApiParam("The maximum number of results to return in a single response page. Leverage the page tokens to iterate through the entire collection.")  Integer maxResults,@QueryParam("pageToken")  @ApiParam("Page token, returned by a previous call, to request the next page of results")  String pageToken) {
        return Response.ok().entity("magic!").build();
    }

    @PATCH
    @Path("/{projectId}/datasets/{datasetId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Updates information in an existing dataset. The update method replaces the entire dataset resource, whereas the patch method only replaces fields that are provided in the submitted dataset resource. This method supports RFC5789 patch semantics.", response = Dataset.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "datasets" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Dataset.class)
    })
    public Response bigqueryDatasetsPatch(@PathParam("projectId") @ApiParam("Required. Project ID of the dataset being updated") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the dataset being updated") String datasetId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid Dataset dataset) {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/{projectId}/datasets/{datasetId}:undelete")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Undeletes a dataset which is within time travel window based on datasetId. If a time is specified, the dataset version deleted at that time is undeleted, else the last live version is undeleted.", response = Dataset.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "datasets" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Dataset.class)
    })
    public Response bigqueryDatasetsUndelete(@PathParam("projectId") @ApiParam("Required. Project ID of the dataset to be undeleted") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of dataset being deleted") String datasetId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid UndeleteDatasetRequest undeleteDatasetRequest) {
        return Response.ok().entity("magic!").build();
    }

    @PUT
    @Path("/{projectId}/datasets/{datasetId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Updates information in an existing dataset. The update method replaces the entire dataset resource, whereas the patch method only replaces fields that are provided in the submitted dataset resource.", response = Dataset.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "datasets" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Dataset.class)
    })
    public Response bigqueryDatasetsUpdate(@PathParam("projectId") @ApiParam("Required. Project ID of the dataset being updated") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the dataset being updated") String datasetId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid Dataset dataset) {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/{projectId}/jobs/{jobId}/cancel")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Requests that a job be cancelled. This call will return immediately, and the client will need to poll for the job status to see if the cancel completed successfully. Cancelled jobs may still incur costs.", response = JobCancelResponse.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = JobCancelResponse.class)
    })
    public Response bigqueryJobsCancel(@PathParam("projectId") @ApiParam("Required. Project ID of the job to cancel") String projectId,@PathParam("jobId") @ApiParam("Required. Job ID of the job to cancel") String jobId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("location")  @ApiParam("The geographic location of the job. You must specify the location to run the job for the following scenarios: - If the location to run a job is not in the &#x60;us&#x60; or the &#x60;eu&#x60; multi-regional location - If the job&#39;s location is in a single region (for example, &#x60;us-central1&#x60;) For more information, see https://cloud.google.com/bigquery/docs/locations#specifying_your_location.")  String location) {
        return Response.ok().entity("magic!").build();
    }

    @DELETE
    @Path("/{projectId}/jobs/{jobId}/delete")
    @ApiOperation(value = "", notes = "Requests the deletion of the metadata of a job. This call returns when the job's metadata is deleted.", response = Void.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Void.class)
    })
    public Response bigqueryJobsDelete(@PathParam("projectId") @ApiParam("Required. Project ID of the job for which metadata is to be deleted.") String projectId,@PathParam("jobId") @ApiParam("Required. Job ID of the job for which metadata is to be deleted. If this is a parent job which has child jobs, the metadata from all child jobs will be deleted as well. Direct deletion of the metadata of child jobs is not allowed.") String jobId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("location")  @ApiParam("The geographic location of the job. Required. See details at: https://cloud.google.com/bigquery/docs/locations#specifying_your_location.")  String location) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/jobs/{jobId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Returns information about a specific job. Job information is available for a six month period after creation. Requires that you're the person who ran the job, or have the Is Owner project role.", response = Job.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Job.class)
    })
    public Response bigqueryJobsGet(@PathParam("projectId") @ApiParam("Required. Project ID of the requested job.") String projectId,@PathParam("jobId") @ApiParam("Required. Job ID of the requested job.") String jobId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("location")  @ApiParam("The geographic location of the job. You must specify the location to run the job for the following scenarios: - If the location to run a job is not in the &#x60;us&#x60; or the &#x60;eu&#x60; multi-regional location - If the job&#39;s location is in a single region (for example, &#x60;us-central1&#x60;) For more information, see https://cloud.google.com/bigquery/docs/locations#specifying_your_location.")  String location) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/queries/{jobId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "RPC to get the results of a query job.", response = GetQueryResultsResponse.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = GetQueryResultsResponse.class)
    })
    public Response bigqueryJobsGetQueryResults(@PathParam("projectId") @ApiParam("Required. Project ID of the query job.") String projectId,@PathParam("jobId") @ApiParam("Required. Job ID of the query job.") String jobId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("formatOptions.useInt64Timestamp")  @ApiParam("Optional. Output timestamp as usec int64. Default is false.")  Boolean formatOptionsUseInt64Timestamp,@QueryParam("location")  @ApiParam("The geographic location of the job. You must specify the location to run the job for the following scenarios: - If the location to run a job is not in the &#x60;us&#x60; or the &#x60;eu&#x60; multi-regional location - If the job&#39;s location is in a single region (for example, &#x60;us-central1&#x60;) For more information, see https://cloud.google.com/bigquery/docs/locations#specifying_your_location.")  String location,@QueryParam("maxResults")  @ApiParam("Maximum number of results to read.")  Integer maxResults,@QueryParam("pageToken")  @ApiParam("Page token, returned by a previous call, to request the next page of results.")  String pageToken,@QueryParam("startIndex")  @ApiParam("Zero-based index of the starting row.")  String startIndex,@QueryParam("timeoutMs")  @ApiParam("Optional: Specifies the maximum amount of time, in milliseconds, that the client is willing to wait for the query to complete. By default, this limit is 10 seconds (10,000 milliseconds). If the query is complete, the jobComplete field in the response is true. If the query has not yet completed, jobComplete is false. You can request a longer timeout period in the timeoutMs field. However, the call is not guaranteed to wait for the specified timeout; it typically returns after around 200 seconds (200,000 milliseconds), even if the query is not complete. If jobComplete is false, you can continue to wait for the query to complete by calling the getQueryResults method until the jobComplete field in the getQueryResults response is true.")  Integer timeoutMs) {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/{projectId}/jobs")
    @Consumes({ "application/octet-stream" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Starts a new asynchronous job. This API has two different kinds of endpoint URIs, as this method supports a variety of use cases. * The *Metadata* URI is used for most interactions, as it accepts the job configuration directly. * The *Upload* URI is ONLY for the case when you're sending both a load job configuration and a data stream together. In this case, the Upload URI accepts the job configuration and the data as two distinct multipart MIME parts.", response = Job.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Job.class)
    })
    public Response bigqueryJobsInsert(@PathParam("projectId") @ApiParam("Project ID of project that will be billed for the job.") String projectId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid Job job) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/jobs")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Lists all jobs that you started in the specified project. Job information is available for a six month period after creation. The job list is sorted in reverse chronological order, by job creation time. Requires the Can View project role, or the Is Owner project role if you set the allUsers property.", response = JobList.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = JobList.class)
    })
    public Response bigqueryJobsList(@PathParam("projectId") @ApiParam("Project ID of the jobs to list.") String projectId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("allUsers")  @ApiParam("Whether to display jobs owned by all users in the project. Default False.")  Boolean allUsers,@QueryParam("maxCreationTime")  @ApiParam("Max value for job creation time, in milliseconds since the POSIX epoch. If set, only jobs created before or at this timestamp are returned.")  String maxCreationTime,@QueryParam("maxResults")  @ApiParam("The maximum number of results to return in a single response page. Leverage the page tokens to iterate through the entire collection.")  Integer maxResults,@QueryParam("minCreationTime")  @ApiParam("Min value for job creation time, in milliseconds since the POSIX epoch. If set, only jobs created after or at this timestamp are returned.")  String minCreationTime,@QueryParam("pageToken")  @ApiParam("Page token, returned by a previous call, to request the next page of results.")  String pageToken,@QueryParam("parentJobId")  @ApiParam("If set, show only child jobs of the specified parent. Otherwise, show all top-level jobs.")  String parentJobId,@QueryParam("projection")  @ApiParam("Restrict information returned to a set of selected fields")  String projection,@QueryParam("stateFilter")  @ApiParam("Filter for job state")  List<String> stateFilter) {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/{projectId}/queries")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Runs a BigQuery SQL query synchronously and returns query results if the query completes within a specified timeout.", response = QueryResponse.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = QueryResponse.class)
    })
    public Response bigqueryJobsQuery(@PathParam("projectId") @ApiParam("Required. Project ID of the query request.") String projectId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid QueryRequest queryRequest) {
        return Response.ok().entity("magic!").build();
    }

    @DELETE
    @Path("/{projectId}/datasets/{datasetId}/models/{modelId}")
    @ApiOperation(value = "", notes = "Deletes the model specified by modelId from the dataset.", response = Void.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "models" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Void.class)
    })
    public Response bigqueryModelsDelete(@PathParam("projectId") @ApiParam("Required. Project ID of the model to delete.") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the model to delete.") String datasetId,@PathParam("modelId") @ApiParam("Required. Model ID of the model to delete.") String modelId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/datasets/{datasetId}/models/{modelId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Gets the specified model resource by model ID.", response = Model.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "models" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Model.class)
    })
    public Response bigqueryModelsGet(@PathParam("projectId") @ApiParam("Required. Project ID of the requested model.") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the requested model.") String datasetId,@PathParam("modelId") @ApiParam("Required. Model ID of the requested model.") String modelId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/datasets/{datasetId}/models")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Lists all models in the specified dataset. Requires the READER dataset role. After retrieving the list of models, you can get information about a particular model by calling the models.get method.", response = ListModelsResponse.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "models" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ListModelsResponse.class)
    })
    public Response bigqueryModelsList(@PathParam("projectId") @ApiParam("Required. Project ID of the models to list.") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the models to list.") String datasetId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("maxResults")  @ApiParam("The maximum number of results to return in a single response page. Leverage the page tokens to iterate through the entire collection.")  Integer maxResults,@QueryParam("pageToken")  @ApiParam("Page token, returned by a previous call to request the next page of results")  String pageToken) {
        return Response.ok().entity("magic!").build();
    }

    @PATCH
    @Path("/{projectId}/datasets/{datasetId}/models/{modelId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Patch specific fields in the specified model.", response = Model.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "models" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Model.class)
    })
    public Response bigqueryModelsPatch(@PathParam("projectId") @ApiParam("Required. Project ID of the model to patch.") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the model to patch.") String datasetId,@PathParam("modelId") @ApiParam("Required. Model ID of the model to patch.") String modelId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid Model model) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/serviceAccount")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "RPC to get the service account for a project used for interactions with Google Cloud KMS", response = GetServiceAccountResponse.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "projects" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = GetServiceAccountResponse.class)
    })
    public Response bigqueryProjectsGetServiceAccount(@PathParam("projectId") @ApiParam("Required. ID of the project.") String projectId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "RPC to list projects to which the user has been granted any project role. Users of this method are encouraged to consider the [Resource Manager](https://cloud.google.com/resource-manager/docs/) API, which provides the underlying data for this method and has more capabilities.", response = ProjectList.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "projects" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ProjectList.class)
    })
    public Response bigqueryProjectsList(@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("maxResults")  @ApiParam("&#x60;maxResults&#x60; unset returns all results, up to 50 per page. Additionally, the number of projects in a page may be fewer than &#x60;maxResults&#x60; because projects are retrieved and then filtered to only projects with the BigQuery API enabled.")  Integer maxResults,@QueryParam("pageToken")  @ApiParam("Page token, returned by a previous call, to request the next page of results. If not present, no further pages are present.")  String pageToken) {
        return Response.ok().entity("magic!").build();
    }

    @DELETE
    @Path("/{projectId}/datasets/{datasetId}/routines/{routineId}")
    @ApiOperation(value = "", notes = "Deletes the routine specified by routineId from the dataset.", response = Void.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "routines" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Void.class)
    })
    public Response bigqueryRoutinesDelete(@PathParam("projectId") @ApiParam("Required. Project ID of the routine to delete") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the routine to delete") String datasetId,@PathParam("routineId") @ApiParam("Required. Routine ID of the routine to delete") String routineId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/datasets/{datasetId}/routines/{routineId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Gets the specified routine resource by routine ID.", response = Routine.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "routines" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Routine.class)
    })
    public Response bigqueryRoutinesGet(@PathParam("projectId") @ApiParam("Required. Project ID of the requested routine") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the requested routine") String datasetId,@PathParam("routineId") @ApiParam("Required. Routine ID of the requested routine") String routineId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("readMask")  @ApiParam("If set, only the Routine fields in the field mask are returned in the response. If unset, all Routine fields are returned.")  String readMask) {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/{projectId}/datasets/{datasetId}/routines")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Creates a new routine in the dataset.", response = Routine.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "routines" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Routine.class)
    })
    public Response bigqueryRoutinesInsert(@PathParam("projectId") @ApiParam("Required. Project ID of the new routine") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the new routine") String datasetId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid Routine routine) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/datasets/{datasetId}/routines")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Lists all routines in the specified dataset. Requires the READER dataset role.", response = ListRoutinesResponse.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "routines" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ListRoutinesResponse.class)
    })
    public Response bigqueryRoutinesList(@PathParam("projectId") @ApiParam("Required. Project ID of the routines to list") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the routines to list") String datasetId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("filter")  @ApiParam("If set, then only the Routines matching this filter are returned. The supported format is &#x60;routineType:{RoutineType}&#x60;, where &#x60;{RoutineType}&#x60; is a RoutineType enum. For example: &#x60;routineType:SCALAR_FUNCTION&#x60;.")  String filter,@QueryParam("maxResults")  @ApiParam("The maximum number of results to return in a single response page. Leverage the page tokens to iterate through the entire collection.")  Integer maxResults,@QueryParam("pageToken")  @ApiParam("Page token, returned by a previous call, to request the next page of results")  String pageToken,@QueryParam("readMask")  @ApiParam("If set, then only the Routine fields in the field mask, as well as project_id, dataset_id and routine_id, are returned in the response. If unset, then the following Routine fields are returned: etag, project_id, dataset_id, routine_id, routine_type, creation_time, last_modified_time, and language.")  String readMask) {
        return Response.ok().entity("magic!").build();
    }

    @PUT
    @Path("/{projectId}/datasets/{datasetId}/routines/{routineId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Updates information in an existing routine. The update method replaces the entire Routine resource.", response = Routine.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "routines" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Routine.class)
    })
    public Response bigqueryRoutinesUpdate(@PathParam("projectId") @ApiParam("Required. Project ID of the routine to update") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the routine to update") String datasetId,@PathParam("routineId") @ApiParam("Required. Routine ID of the routine to update") String routineId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid Routine routine) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/datasets/{datasetId}/tables/{tableId}/rowAccessPolicies")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Lists all row access policies on the specified table.", response = ListRowAccessPoliciesResponse.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "rowAccessPolicies" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ListRowAccessPoliciesResponse.class)
    })
    public Response bigqueryRowAccessPoliciesList(@PathParam("projectId") @ApiParam("Required. Project ID of the row access policies to list.") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of row access policies to list.") String datasetId,@PathParam("tableId") @ApiParam("Required. Table ID of the table to list row access policies.") String tableId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("pageSize")  @ApiParam("The maximum number of results to return in a single response page. Leverage the page tokens to iterate through the entire collection.")  Integer pageSize,@QueryParam("pageToken")  @ApiParam("Page token, returned by a previous call, to request the next page of results.")  String pageToken) {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/{projectId}/datasets/{datasetId}/tables/{tableId}/insertAll")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Streams data into BigQuery one record at a time without needing to run a load job.", response = TableDataInsertAllResponse.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "tabledata" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = TableDataInsertAllResponse.class)
    })
    public Response bigqueryTabledataInsertAll(@PathParam("projectId") @ApiParam("Required. Project ID of the destination.") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the destination.") String datasetId,@PathParam("tableId") @ApiParam("Required. Table ID of the destination.") String tableId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid TableDataInsertAllRequest tableDataInsertAllRequest) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/datasets/{datasetId}/tables/{tableId}/data")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "List the content of a table in rows.", response = TableDataList.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "tabledata" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = TableDataList.class)
    })
    public Response bigqueryTabledataList(@PathParam("projectId") @ApiParam("Required. Project id of the table to list.") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset id of the table to list.") String datasetId,@PathParam("tableId") @ApiParam("Required. Table id of the table to list.") String tableId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("formatOptions.useInt64Timestamp")  @ApiParam("Optional. Output timestamp as usec int64. Default is false.")  Boolean formatOptionsUseInt64Timestamp,@QueryParam("maxResults")  @ApiParam("Row limit of the table.")  Integer maxResults,@QueryParam("pageToken")  @ApiParam("To retrieve the next page of table data, set this field to the string provided in the pageToken field of the response body from your previous call to tabledata.list.")  String pageToken,@QueryParam("selectedFields")  @ApiParam("Subset of fields to return, supports select into sub fields. Example: selected_fields &#x3D; \&quot;a,e.d.f\&quot;;")  String selectedFields,@QueryParam("startIndex")  @ApiParam("Start row index of the table.")  String startIndex) {
        return Response.ok().entity("magic!").build();
    }

    @DELETE
    @Path("/{projectId}/datasets/{datasetId}/tables/{tableId}")
    @ApiOperation(value = "", notes = "Deletes the table specified by tableId from the dataset. If the table contains data, all the data will be deleted.", response = Void.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "tables" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Void.class)
    })
    public Response bigqueryTablesDelete(@PathParam("projectId") @ApiParam("Required. Project ID of the table to delete") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the table to delete") String datasetId,@PathParam("tableId") @ApiParam("Required. Table ID of the table to delete") String tableId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/datasets/{datasetId}/tables/{tableId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Gets the specified table resource by table ID. This method does not return the data in the table, it only returns the table resource, which describes the structure of this table.", response = Table.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "tables" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Table.class)
    })
    public Response bigqueryTablesGet(@PathParam("projectId") @ApiParam("Required. Project ID of the requested table") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the requested table") String datasetId,@PathParam("tableId") @ApiParam("Required. Table ID of the requested table") String tableId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("selectedFields")  @ApiParam("List of table schema fields to return (comma-separated). If unspecified, all fields are returned. A fieldMask cannot be used here because the fields will automatically be converted from camelCase to snake_case and the conversion will fail if there are underscores. Since these are fields in BigQuery table schemas, underscores are allowed.")  String selectedFields,@QueryParam("view")  @ApiParam("Optional. Specifies the view that determines which table information is returned. By default, basic table information and storage statistics (STORAGE_STATS) are returned.")  String view) {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/{projectId}/datasets/{datasetId}/tables")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Creates a new, empty table in the dataset.", response = Table.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "tables" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Table.class)
    })
    public Response bigqueryTablesInsert(@PathParam("projectId") @ApiParam("Required. Project ID of the new table") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the new table") String datasetId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@Valid Table table) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{projectId}/datasets/{datasetId}/tables")
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Lists all tables in the specified dataset. Requires the READER dataset role.", response = TableList.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "tables" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = TableList.class)
    })
    public Response bigqueryTablesList(@PathParam("projectId") @ApiParam("Required. Project ID of the tables to list") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the tables to list") String datasetId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("maxResults")  @ApiParam("The maximum number of results to return in a single response page. Leverage the page tokens to iterate through the entire collection.")  Integer maxResults,@QueryParam("pageToken")  @ApiParam("Page token, returned by a previous call, to request the next page of results")  String pageToken) {
        return Response.ok().entity("magic!").build();
    }

    @PATCH
    @Path("/{projectId}/datasets/{datasetId}/tables/{tableId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Updates information in an existing table. The update method replaces the entire table resource, whereas the patch method only replaces fields that are provided in the submitted table resource. This method supports RFC5789 patch semantics.", response = Table.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "tables" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Table.class)
    })
    public Response bigqueryTablesPatch(@PathParam("projectId") @ApiParam("Required. Project ID of the table to update") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the table to update") String datasetId,@PathParam("tableId") @ApiParam("Required. Table ID of the table to update") String tableId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("autodetect_schema")  @ApiParam("Optional.  When true will autodetect schema, else will keep original schema")  Boolean autodetectSchema,@Valid Table table) {
        return Response.ok().entity("magic!").build();
    }

    @PUT
    @Path("/{projectId}/datasets/{datasetId}/tables/{tableId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Updates information in an existing table. The update method replaces the entire Table resource, whereas the patch method only replaces fields that are provided in the submitted Table resource.", response = Table.class, authorizations = {
            @Authorization(value = "Oauth2c", scopes = {
            }),
            @Authorization(value = "Oauth2", scopes = {
            })
    }, tags={ "tables" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Table.class)
    })
    public Response bigqueryTablesUpdate(@PathParam("projectId") @ApiParam("Required. Project ID of the table to update") String projectId,@PathParam("datasetId") @ApiParam("Required. Dataset ID of the table to update") String datasetId,@PathParam("tableId") @ApiParam("Required. Table ID of the table to update") String tableId,@QueryParam("$.xgafv")  @ApiParam("V1 error format.")  String $xgafv,@QueryParam("access_token")  @ApiParam("OAuth access token.")  String accessToken,@QueryParam("alt")  @ApiParam("Data format for response.")  String alt,@QueryParam("callback")  @ApiParam("JSONP")  String paramCallback,@QueryParam("fields")  @ApiParam("Selector specifying which fields to include in a partial response.")  String fields,@QueryParam("key")  @ApiParam("API key. Your API key identifies your project and provides you with API access, quota, and reports. Required unless you provide an OAuth 2.0 token.")  String key,@QueryParam("oauth_token")  @ApiParam("OAuth 2.0 token for the current user.")  String oauthToken,@QueryParam("prettyPrint")  @ApiParam("Returns response with indentations and line breaks.")  Boolean prettyPrint,@QueryParam("quotaUser")  @ApiParam("Available to use for quota purposes for server-side applications. Can be any arbitrary string assigned to a user, but should not exceed 40 characters.")  String quotaUser,@QueryParam("upload_protocol")  @ApiParam("Upload protocol for media (e.g. \&quot;raw\&quot;, \&quot;multipart\&quot;).")  String uploadProtocol,@QueryParam("uploadType")  @ApiParam("Legacy upload protocol for media (e.g. \&quot;media\&quot;, \&quot;multipart\&quot;).")  String uploadType,@QueryParam("autodetect_schema")  @ApiParam("Optional.  When true will autodetect schema, else will keep original schema")  Boolean autodetectSchema,@Valid Table table) {
        return Response.ok().entity("magic!").build();
    }
}
