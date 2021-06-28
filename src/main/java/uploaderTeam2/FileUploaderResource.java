package uploaderTeam2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import uploaderTeam2.models.FileUploader;



@Path("files")
@Stateless
public class FileUploaderResource {
	@Context
	protected UriInfo context;
	
	@PersistenceContext(unitName = "filesDatabase")
	private EntityManager em;
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getRandomFile() {

		Long amountOfFiles = em.createQuery("SELECT COUNT(f) FROM FileUpload f", Long.class).getSingleResult();
		Long randomPrimaryKey;

		if (amountOfFiles == null || amountOfFiles == 0) {
			return Response.ok().build();
		} else if (amountOfFiles == 1) {
			randomPrimaryKey = 1L;
		} else {
			randomPrimaryKey = ThreadLocalRandom.current().nextLong(1, amountOfFiles + 1);
		}

	FileUploader randomFile = em.find(FileUploader.class, randomPrimaryKey);

		return Response.ok(randomFile.getData(), MediaType.APPLICATION_OCTET_STREAM)
				.header("Content-Disposition", "attachment;filename=\"" + randomFile.getFileName() + "\"").build();		
	}




@GET 
@Path("{id}")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public Response getFile(@PathParam("id") long id) {
	FileUploader file = em.find(FileUploader.class, id);

	return Response.ok(file.getData(), MediaType.APPLICATION_OCTET_STREAM)
			.header("Content-Disposition", "attachment;filename=\"" + file.getFileName() + "\"").build();		
}


@POST
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.TEXT_PLAIN)
public String uploadFile(MultipartFormDataInput incomingFile) throws IOException {

	InputPart inputPart = incomingFile.getFormDataMap().get("file").get(0);
	InputStream uploadedInputStream = inputPart.getBody(InputStream.class, null);

	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	byte[] buffer = new byte[1024];
	int len;

	while ((len = uploadedInputStream.read(buffer)) != -1) {
		byteArrayOutputStream.write(buffer, 0, len);
	}

	FileUploader upload = new FileUploader(
			getFileNameOfUploadedFile(inputPart.getHeaders().getFirst("Content-Disposition")),
			getContentTypeOfUploadedFile(inputPart.getHeaders().getFirst("Content-Type")),
			byteArrayOutputStream.toByteArray());

	return context.getRequestUri().toString() + "/" +em.merge(upload).getId();
	 
}




/****/
private String getFileNameOfUploadedFile(String contentDispositionHeader) {

	if (contentDispositionHeader == null || contentDispositionHeader.isEmpty()) {
		return "unkown";
	} else {
		String[] contentDispositionHeaderTokens = contentDispositionHeader.split(";");

		for (String contentDispositionHeaderToken : contentDispositionHeaderTokens) {
			if ((contentDispositionHeaderToken.trim().startsWith("filename"))) {
				return contentDispositionHeaderToken.split("=")[1].trim().replaceAll("\"", "");
			}
		}

		return "unkown";
	}
}


private String getContentTypeOfUploadedFile(String contentTypeHeader) {
	if (contentTypeHeader == null || contentTypeHeader.isEmpty()) {
		return "unkown";
	} else {
		return contentTypeHeader.replace("[", "").replace("]", "");
	}
}

}

