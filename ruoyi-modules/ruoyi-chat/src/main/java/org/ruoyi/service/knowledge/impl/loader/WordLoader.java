package org.ruoyi.service.knowledge.impl.loader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.knowledge.TextSplitter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class WordLoader implements ResourceLoader {
    private final TextSplitter textSplitter;

    @Override
    public String getContent(InputStream inputStream) {
        try {
            InputStream checkedStream = FileMagic.prepareToCheckMagic(inputStream);
            FileMagic magic = FileMagic.valueOf(checkedStream);
            if (magic == FileMagic.OLE2) {
                try (HWPFDocument document = new HWPFDocument(checkedStream);
                     WordExtractor extractor = new WordExtractor(document)) {
                    return extractor.getText();
                }
            }
            try (XWPFDocument document = new XWPFDocument(checkedStream);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getChunkList(String content, String kid) {
        return textSplitter.split(content, kid);
    }

}
