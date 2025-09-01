package com.itcareer.media.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@ApiModel
public class UploadCertificateForm {
  @NotEmpty(message = "username cannot be null")
  @ApiModelProperty(name = "username", required = true)
  private String username;
  @NotEmpty(message = "simulationName cannot be null")
  @ApiModelProperty(name = "simulationName", required = true)
  private String simulationName;
}
