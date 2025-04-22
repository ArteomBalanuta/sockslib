package sockslib.hexanalyser;

import java.util.*;

class HexNode {
    private final Map<String, HexNode> children = new HashMap<>();
    private int endCount;

    public Map<String, HexNode> getChildren() { return children; }
    public int getEndCount() { return endCount; }
    public void incrementEndCount() { endCount++; }
}

public class HexTrieAnalyzer {
    private final HexNode root = new HexNode();

    public void insert(String hexString) {
        List<String> parts = splitHexString(hexString);
        HexNode current = root;

        for (String part : parts) {
            current = current.getChildren()
                    .computeIfAbsent(part, k -> new HexNode());
        }
        current.incrementEndCount();
    }

    private List<String> splitHexString(String hex) {
        String cleanHex = hex.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        List<String> parts = new ArrayList<>();

        for (int i = 0; i < cleanHex.length(); i += 2) {
            int end = Math.min(i + 2, cleanHex.length());
            parts.add(cleanHex.substring(i, end));
        }
        return parts;
    }


    public void analyze() {
        List<String> unique = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        traverseTree(root, new ArrayList<>(), unique, duplicates);

        System.out.println("Unique payloads (" + unique.size() + "):");
        unique.forEach(System.out::println);

        System.out.println("\nDuplicate payloads (" + duplicates.size() + "):");
        duplicates.forEach(System.out::println);
    }

    private void traverseTree(HexNode node, List<String> path,
                              List<String> unique, List<String> duplicates) {
        if (node.getEndCount() > 0) {
            String fullPath = String.join("", path);
            if (node.getEndCount() == 1) {
                unique.add(fullPath);
            } else {
                duplicates.add(fullPath + " (count: " + node.getEndCount() + ")");
            }
        }

        for (Map.Entry<String, HexNode> entry : node.getChildren().entrySet()) {
            path.add(entry.getKey());
            traverseTree(entry.getValue(), path, unique, duplicates);
            path.remove(path.size() - 1);
        }
    }
//63A5B34D3C67A6
//63A5B35E065D9A (count: 4)
//63A5B34D3F64A5 (count: 4)
    public static void main(String[] args) {
        // Example input data
        String[] payloads = {
                "63A4B6A3B07E",
                "63A1E0",
                "63A1D3",
                "63A1D3",
                "63A7BA4123",
                "63A1D3",
                "63A1D3",
                "63A5B35E613AFC",
                "63A4B6A3B1A3",
                "63A1D3",
                "63A1D3",
                "63A5B35E613AFC",
                "63AAB6A0B0B7B07E",
                "63A1D3",
                "63A5B35E613AFC",
                "63A1D3",
                "63ABBAB185A5DAFD24",
                "63A5B35E613AFC",
                "63A1D3",
                "63AA76A2CA1BDF82",
                "63A1D3",
                "60A2ADB1A3B07EA2A01025102582A2",
                "63A5B35E613AFC",
                "63A1D3",
                "63A1D3",
                "63A7BA4426",
                "63A4B6A3A2CD",
                "63A4A79A0023",
                "63A1D3",
                "63A1D3",
                "63A4A79B0221",

        };

        HexTrieAnalyzer analyzer = new HexTrieAnalyzer();
        for (String payload : payloads) {
            analyzer.insert(payload);
        }

        analyzer.analyze();
    }
}