package org.change.v2.p4.model.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class P4Graph {
    private List<P4Node> nodes = new ArrayList<>();
    private List<P4Edge> edges = new ArrayList<>();

    public List<P4Node> getNodes() {
        return nodes;
    }

    public List<P4Edge> getEdges() {
        return edges;
    }

    public static P4Graph fromYaml(String file) throws IOException {
        InputStream fis = new FileInputStream(file);
        P4Graph p4g = fromYaml(fis);
        fis.close();
        return p4g;
    }

    public static P4Graph fromYaml(InputStream is) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(is, P4Graph.class);
    }

    @Override
    public String toString() {
        return Objects.toString(getNodes()) + ";\n" + Objects.toString(getEdges());
    }

    public static void main(String[] args) throws IOException {
        System.out.println(fromYaml(new FileInputStream("inputs/simple-nat-modular/integration.yaml")));
    }
}
