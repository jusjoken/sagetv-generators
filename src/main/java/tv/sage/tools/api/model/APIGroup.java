package tv.sage.tools.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by seans on 12/03/17.
 */
public class APIGroup
{
  private String className;
  private String fileName;
  private String packageName;
  private String forType;
  private String comment;
  private List<APIMethod> methods = new ArrayList<>();
  private String forIdMethod;
  private String isTypeMethod;
  private String getIdMethod;

  private boolean serializable;

  public APIGroup()
  {
  }

  public String getIsTypeMethod()
  {
    return isTypeMethod;
  }

  public void setIsTypeMethod(String isTypeMethod)
  {
    this.isTypeMethod = isTypeMethod;
  }


  public String getClassName()
  {
    return className;
  }

  public APIGroup setClassName(String className)
  {
    this.className = className;
    return this;
  }

  public String getFileName()
  {
    return fileName;
  }

  public APIGroup setFileName(String fileName)
  {
    this.fileName = fileName;
    return this;
  }

  public String getPackageName()
  {
    return packageName;
  }

  public APIGroup setPackageName(String packageName)
  {
    this.packageName = packageName;
    return this;
  }

  public String getForType()
  {
    return forType;
  }

  public APIGroup setForType(String forType)
  {
    this.forType = forType;
    return this;
  }

  public String getComment()
  {
    return comment;
  }

  public APIGroup setComment(String comment)
  {
    this.comment = comment;
    return this;
  }

  public List<APIMethod> getMethods()
  {
    return methods;
  }

  public APIGroup setMethods(List<APIMethod> methods)
  {
    this.methods = methods;
    return this;
  }

  public String getForIdMethod()
  {
    return forIdMethod;
  }

  public APIGroup setForIdMethod(String forIdMethod)
  {
    this.forIdMethod = forIdMethod;
    return this;
  }

  public void setSerializable(boolean serializable)
  {
    this.serializable = serializable;
  }

  public boolean isSerializable()
  {
    return serializable;
  }

  public boolean hasForIDMethod()
  {
    return getForIdMethod()!=null && getForIdMethod().trim().length()>0;
  }

  public String getGetIdMethod()
  {
    return getIdMethod;
  }

  public APIGroup setGetIdMethod(String getIdMethod)
  {
    this.getIdMethod = getIdMethod;
    return this;
  }
}
