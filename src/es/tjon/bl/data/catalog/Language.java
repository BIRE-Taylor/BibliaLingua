package es.tjon.bl.data.catalog;

import com.mobandme.ada.Entity;
import com.mobandme.ada.annotations.Table;
import com.mobandme.ada.annotations.TableField;

@Table(name = "Language")
public class Language extends Entity
{
	@TableField(name = "l_id", datatype = DATATYPE_INTEGER)
	public int id = -1;
	@TableField(name = "l_lssXmlCode", datatype = DATATYPE_INTEGER)
	public int lds_xml_code = -1;
	@TableField(name = "l_androidSdkVersion", datatype = DATATYPE_INTEGER)
	public int android_sdk_version = -1;
	@TableField(name = "l_name", datatype = DATATYPE_TEXT)
	public String name = null;
	@TableField(name = "l_engName", datatype = DATATYPE_TEXT)
	public String eng_name = null;
	@TableField(name = "l_code", datatype = DATATYPE_TEXT)
	public String code = null;
	@TableField(name = "l_codeThree", datatype = DATATYPE_TEXT)
	public String code_three = null;
}
