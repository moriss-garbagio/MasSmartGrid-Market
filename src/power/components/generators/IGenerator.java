package power.components.generators;

import power.tools.IDescribable;

public interface IGenerator<T> extends IDescribable {
	public T create();
}
