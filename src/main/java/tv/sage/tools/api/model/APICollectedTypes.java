package tv.sage.tools.api.model;

import java.util.Set;
import java.util.TreeSet;

/**
 * Created by seans on 18/03/17.
 */
public class APICollectedTypes
{
  public APICollectedTypes() {}

  Set<String> sageTypes = new TreeSet<>();
  Set<String> primitiveTypes = new TreeSet<>();
  Set<String> nonPrimitiveTypes = new TreeSet<>();

  public Set<String> getSageTypes()
  {
    return sageTypes;
  }

  public Set<String> getPrimitiveTypes()
  {
    return primitiveTypes;
  }

  public Set<String> getNonPrimitiveTypes()
  {
    return nonPrimitiveTypes;
  }

}
