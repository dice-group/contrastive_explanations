package tools;

import java.util.Objects;

public class Pair<V,H> {
    private V key;
    private H value;

    public Pair(V key, H value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(key, pair.key) && Objects.equals(value, pair.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    public V getKey() {
        return key;
    }

    public void setKey(V key) {
        this.key = key;
    }

    public H getValue() {
        return value;
    }

    public void setValue(H value) {
        this.value = value;
    }
    /**
     * <p><code>String</code> representation of this
     * <code>Pair</code>.</p>
     *
     * <p>The default name/value delimiter '=' is always used.</p>
     *
     *  @return <code>String</code> representation of this <code>Pair</code>
     */
    @Override
    public String toString() {
        return key + "=" + value;
    }
}
