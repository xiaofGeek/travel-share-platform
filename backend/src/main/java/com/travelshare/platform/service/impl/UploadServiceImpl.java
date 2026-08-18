package com.travelshare.platform.service.impl;

import com.travelshare.platform.exception.BusinessException;
import com.travelshare.platform.service.UploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class UploadServiceImpl implements UploadService {
    private final Path root;
    private final long maxBytes;
    private final Set<String> allowed;
    public UploadServiceImpl(@Value("${app.upload.directory:uploads}") String directory,
                             @Value("${app.upload.max-bytes:10485760}") long maxBytes,
                             @Value("${app.upload.allowed-extensions:jpg,jpeg,png,webp,gif}") String extensions) {
        this.root=Path.of(directory).toAbsolutePath().normalize(); this.maxBytes=maxBytes;
        this.allowed=new HashSet<>(Arrays.asList(extensions.toLowerCase(Locale.ROOT).split(",")));
    }
    @Override public Map<String,Object> upload(MultipartFile file,String category){if(file==null||file.isEmpty())throw BusinessException.badRequest("请选择图片文件");if(file.getSize()>maxBytes)throw BusinessException.badRequest("图片不能超过 10MB");String original=Optional.ofNullable(file.getOriginalFilename()).orElse("image");String extension=original.contains(".")?original.substring(original.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";if(!allowed.contains(extension))throw BusinessException.badRequest("仅支持 jpg、png、webp 或 gif 图片");String safeCategory=category==null?"common":category.replaceAll("[^a-zA-Z0-9_-]","");if(safeCategory.isBlank())safeCategory="common";String date=LocalDate.now().toString();String name=UUID.randomUUID().toString().replace("-","")+"."+extension;Path directory=root.resolve("user").resolve(safeCategory).resolve(date).normalize();Path target=directory.resolve(name).normalize();if(!target.startsWith(root))throw BusinessException.badRequest("上传路径不合法");try{Files.createDirectories(directory);file.transferTo(target);}catch(IOException e){throw new BusinessException(500,"图片保存失败");}String relative="/uploads/user/"+safeCategory+"/"+date+"/"+name;return Map.of("url",relative,"name",name,"size",file.getSize());}
    @Override public void delete(String relativePath){if(relativePath==null||!relativePath.startsWith("/uploads/user/"))throw BusinessException.badRequest("只能删除用户上传文件");Path target=root.resolve(relativePath.substring("/uploads/".length())).normalize();if(!target.startsWith(root))throw BusinessException.badRequest("文件路径不合法");try{Files.deleteIfExists(target);}catch(IOException e){throw new BusinessException(500,"文件删除失败");}}
}

