package ca.allanwang.mcgill.models.bindings

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Created by Allan Wang on 2017-10-29.
 *
 * Base interface from which all models inherit
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
interface McGillModel