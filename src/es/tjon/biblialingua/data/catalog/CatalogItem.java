package es.tjon.biblialingua.data.catalog;

public interface CatalogItem
{

	public String getCoverURL();

	public String getName();
	public int getId();
	public boolean isSelected();
	public void setSelected(boolean selected);
}
