package com.tetrisbot;

// GifSequenceWriter utility adapted from Karthik Kumar’s StackOverflow post:
// https://stackoverflow.com/a/20635536
// Original concept from Elliott Hughes (2005)

import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

public class GifSequenceWriter {
    private ImageWriter writer;
    private ImageWriteParam params;
    private IIOMetadata metadata;
    private ImageOutputStream output;

    public GifSequenceWriter(ImageOutputStream output, int imageType, int delayMs, boolean loop) throws IOException {
        this.output = output;
        writer = ImageIO.getImageWritersBySuffix("gif").next();
        params = writer.getDefaultWriteParam();
        ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);

        metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params);
        String metaFormatName = metadata.getNativeMetadataFormatName();

        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);
        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(delayMs / 10));
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
        IIOMetadataNode appExtension = new IIOMetadataNode("ApplicationExtension");
        appExtension.setAttribute("applicationID", "NETSCAPE");
        appExtension.setAttribute("authenticationCode", "2.0");

        byte[] loopBytes = new byte[]{0x1, (byte) (loop ? 0 : 1), 0};
        appExtension.setUserObject(loopBytes);
        appExtensionsNode.appendChild(appExtension);
        metadata.setFromTree(metaFormatName, root);

        writer.setOutput(output);
        writer.prepareWriteSequence(null);
    }

    public void writeToSequence(RenderedImage img) throws IOException {
        writer.writeToSequence(new IIOImage(img, null, metadata), params);
    }

    public void close() throws IOException {
        writer.endWriteSequence();
        output.close();
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        for (int i = 0; i < rootNode.getLength(); i++) {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return node;
    }
}
