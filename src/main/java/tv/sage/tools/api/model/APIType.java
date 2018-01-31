package tv.sage.tools.api.model;

/**
 * Created by seans on 12/03/17.
 */
public class APIType
{
  private String type;
  private boolean isArray;
  private String name;
  private boolean isPrimitive;
  private boolean isSageObject;

  public APIType()
  {
  }

  public String getType()
  {
    return type;
  }

  public APIType setType(String type)
  {
    this.type = type;
    return this;
  }

  public boolean isArray()
  {
    return isArray;
  }

  public APIType setArray(boolean array)
  {
    isArray = array;
    return this;
  }

  public String getName()
  {
    return name;
  }

  public APIType setName(String name)
  {
    this.name = name;
    return this;
  }

  public void setIsPrimitive(boolean isPrimitive)
  {
    this.isPrimitive = isPrimitive;
  }

  public boolean isPrimitive()
  {
    return isPrimitive;
  }

  public void setIsSageObject(boolean isSageObject)
  {
    this.isSageObject = isSageObject;
  }

  public boolean isSageObject()
  {
    return isSageObject;
  }
}
