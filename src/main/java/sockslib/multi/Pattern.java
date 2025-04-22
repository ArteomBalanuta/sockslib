package sockslib.multi;

public class Pattern {
    public String suffix;
    public String postfix;

    public Pattern(String suffix, String postfix) {
        if (suffix == null) {
            throw new RuntimeException("Suffix should be present!");
        }
        this.suffix = suffix;
        this.postfix = postfix;
    }
}
