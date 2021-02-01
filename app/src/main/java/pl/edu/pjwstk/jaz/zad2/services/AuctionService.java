package pl.edu.pjwstk.jaz.zad2.services;

import com.sun.xml.bind.v2.TODO;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.pjwstk.jaz.zad2.entities.*;
import pl.edu.pjwstk.jaz.zad2.exception.BadCategoryRequestException;
import pl.edu.pjwstk.jaz.zad2.exception.NoAuctionException;
import pl.edu.pjwstk.jaz.zad2.exception.NoCategoryException;
import pl.edu.pjwstk.jaz.zad2.request.AuctionRequest;
import pl.edu.pjwstk.jaz.zad2.request.PhotoRequest;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;

@Transactional
@Service
public class AuctionService {

    EntityManager em;
    CategoryService categoryService;


    public AuctionService(EntityManager em, CategoryService categoryService) {
        this.em = em;
        this.categoryService = categoryService;
    }

    public void createAuction(AuctionRequest auctionRequest) throws NoCategoryException {
        //check if category from request exists in DB
        List<String> availableCategories = categoryService.showCategories();

        List<ParameterEntity> availableParameters = getAllAvailableParametersFromParameterDB();

        int photoPosition = 1;
        //get current logged in user's name
        final Long currentUsersId = getCurrentUsersId();

        //create new auction
        AuctionEntity newAuction = new AuctionEntity();

        //create category object that goes to the auction
        //and check if its not null, otherwise an exception will be thrown
        CategoryEntity category = getCategoryIdWithItsTitle(auctionRequest.getCategory());
        if (category == null)
            throw new NoCategoryException("No such category");

        // set auction parameters
        newAuction.setTitle(auctionRequest.getTitle());
        newAuction.setDescription(auctionRequest.getDescription());

        if (currentUsersId != null)
            newAuction.setCreatorsId(currentUsersId);
        else throw new BadCategoryRequestException("Wrong user id");


        if (availableCategories.contains(category.getTitle()))
            newAuction.setCategoryId(category.getId());
        else throw new NoCategoryException("No such category");

        //add photos' links to a set
        for (String photoTitle : auctionRequest.getPhotos()) {
            newAuction.addPhoto(photoTitle, photoPosition);
            photoPosition++;
        }
        newAuction.setPrice(auctionRequest.getPrice());


        auctionRequest.getParameters().forEach((k, v) -> {
                    ParameterEntity parameterEntity = getParameterEntity(k);
                    if (getParameterEntity(k) == null) {
                        parameterEntity = new ParameterEntity(k);
                        em.persist(parameterEntity);
                    }
                    flushAndClear();
                    AuctionParameterEntity auctionParameterEntity = new AuctionParameterEntity(newAuction, parameterEntity, v);
                    auctionParameterEntity.setAuctionParameterKey(new AuctionParameterPk(newAuction.getId(), parameterEntity.getId()));
                    newAuction.addAuctionParameter(parameterEntity, auctionParameterEntity);
                }
        );
        em.merge(newAuction);
    }


    public void editAuction(Long id, AuctionRequest auctionRequest) {
        AuctionEntity updatedAuction = em.find(AuctionEntity.class, id);
        if (updatedAuction != null && updatedAuction.getCreatorsId().equals(getCurrentUsersId())) {
            List<ParameterEntity> allAuctionParameter = getAllAvailableParametersFromParameterDB();


            if (auctionRequest.getTitle() != null || auctionRequest.getTitle().equals(""))
                updatedAuction.setTitle(auctionRequest.getTitle());
            if (auctionRequest.getCategory() != null)
                updatedAuction.setCategoryId(getCategoryIdWithItsTitle(auctionRequest.getCategory()).getId());
            if (auctionRequest.getDescription() != null)
                updatedAuction.setDescription(auctionRequest.getDescription());
            if (auctionRequest.getPrice() != null)
                updatedAuction.setPrice(auctionRequest.getPrice());

//            em.merge(updatedAuction);

            auctionRequest.getParameters().forEach((k, v) -> {
                AuctionParameterEntity auctionParameterEntity;

                ParameterEntity parameterEntity = getParameterEntity(k);
                if (getParameterEntity(k) == null) {
                    parameterEntity = new ParameterEntity(k);
                    em.persist(parameterEntity);
                }

                em.merge(parameterEntity);
                em.merge(updatedAuction);
                flushAndClear();


                if (getAuctionParameterEntity(updatedAuction.getId(), parameterEntity.getId()) != null) {
                    auctionParameterEntity = getAuctionParameterEntity(updatedAuction.getId(), parameterEntity.getId());
                    auctionParameterEntity.setValue(v);
                    em.merge(auctionParameterEntity);
                } else {
                    auctionParameterEntity = new AuctionParameterEntity(updatedAuction, parameterEntity, v);
                    auctionParameterEntity.setAuctionParameterKey(new AuctionParameterPk(updatedAuction.getId(), parameterEntity.getId()));
                    updatedAuction.addAuctionParameter(parameterEntity, auctionParameterEntity);
                }

                em.merge(updatedAuction);
            });
        } else {
            throw new NoAuctionException("Ni ma aukcji");
        }
    }


    public List<ShowAuctionEntity> showAuctionsWithOnePhoto() {
        //get current logged in user's name
        final Long currentUsersId = getCurrentUsersId();
        List<ShowAuctionEntity> allUsersRefactoredAuctions = new ArrayList<>();
        List<AuctionEntity> allUserAuctions = getAllUsersAuctions(currentUsersId);

        for (AuctionEntity ae : allUserAuctions) {
            allUsersRefactoredAuctions.add(new ShowAuctionEntity(
                    ae.getId(),
                    getCategorysTitleWithItsId(ae.getCategoryId()).getTitle(),
                    ae.getTitle(),
                    ae.getDescription(),
                    getMinaturePhotoTitle(ae.getId()).getLink()
            ));
        }
        return allUsersRefactoredAuctions;
    }

    public void editPhotosInAuction(Long id, PhotoRequest photoRequest) {
        photoRequest.getNewPhotos().forEach((link) -> {
                    AuctionEntity updatedAuction = em.find(AuctionEntity.class, id);
                    if(updatedAuction.getCreatorsId().equals(getCurrentUsersId())) {
                        int numberOfAllPhotos = getAllAuctionsPhoto(id).size();
                        if (getSpecificAuctionsPhoto(id, link) == null) {
                            updatedAuction.addPhoto(link, numberOfAllPhotos + 1);
                        }
                        em.merge(updatedAuction);
                    }else
                        throw new NoAuctionException("Ni twoja aukcja");
                }
        );
    }


    private Long getCurrentUsersId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUsersId = null;
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            currentUsersId = Long.parseLong(authentication.getName());
            System.out.println(currentUsersId);
        }
        return currentUsersId;
    }


    public CategoryEntity getCategoryIdWithItsTitle(String categoryName) {
        try {
            return em.createQuery("SELECT ce FROM CategoryEntity ce WHERE ce.title = :title", CategoryEntity.class)
                    .setParameter("title", categoryName)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            System.out.println("No such category id");
        }
        return null;
    }

    public CategoryEntity getCategorysTitleWithItsId(Long id) {
        try {
            return em.createQuery("SELECT ce FROM CategoryEntity ce WHERE ce.id = :id", CategoryEntity.class)
                    .setParameter("id", id)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            System.out.println("No such category title");
        }
        return null;
    }

    public ParameterEntity getParameterEntity(String parameterName) {
        try {
            return em.createQuery("SELECT pe FROM ParameterEntity pe WHERE pe.key = :keyName", ParameterEntity.class)
                    .setParameter("keyName", parameterName)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public AuctionParameterEntity getAuctionParameterEntity(Long auctionId, Long parameterId) {
        try {
            return em.createQuery("SELECT ape FROM AuctionParameterEntity ape where ape.auctionEntity.id = :auctionId AND ape.parameterEntity.id = :parameterId", AuctionParameterEntity.class)
                    .setParameter("auctionId", auctionId)
                    .setParameter("parameterId", parameterId)
                    .getSingleResult();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    public List<ParameterEntity> getAllAvailableParametersFromParameterDB() {
        try {
            return em.createQuery("SELECT pe FROM ParameterEntity pe", ParameterEntity.class)
                    .getResultList();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public List<AuctionEntity> getAllUsersAuctions(Long id) {
        try {
            return em.createQuery("SELECT ae FROM AuctionEntity ae WHERE ae.creatorsId = :id", AuctionEntity.class)
                    .setParameter("id", id)
                    .getResultList();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public PhotoEntity getMinaturePhotoTitle(Long auctionId) {
        try {
            return em.createQuery("SELECT pe FROM PhotoEntity pe WHERE pe.auctionId = :id order by pe.position", PhotoEntity.class)
                    .setParameter("id", auctionId)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public PhotoEntity getSpecificAuctionsPhoto(Long auctionId, String link) {
        try {
            return em.createQuery("SELECT pe FROM PhotoEntity pe WHERE pe.auctionId = :id AND pe.link = :link", PhotoEntity.class)
                    .setParameter("id", auctionId)
                    .setParameter("link", link)
                    .getSingleResult();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public List<PhotoEntity> getAllAuctionsPhoto(Long auctionId) {
        try {
            return em.createQuery("SELECT pe FROM PhotoEntity pe WHERE pe.auctionId = :id", PhotoEntity.class)
                    .setParameter("id", auctionId)
                    .getResultList();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }


}
