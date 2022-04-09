package template;

import io.cruder.apt.Replica;
import io.cruder.apt.Replica.Literal;
import io.cruder.apt.Replica.Name;
import io.cruder.apt.Replica.TypeRef;
import io.cruder.example.domain.User;
import io.cruder.example.dto.user.UserAddDTO;
import io.cruder.example.dto.user.UserDetailsDTO;
import io.cruder.example.dto.user.UserListItemDTO;
import io.cruder.example.dto.user.UserQueryDTO;
import template.crud.TConverter;
import template.crud.TEntity;
import template.crud.TRepository;
import template.crud.dto.TAddDTO;
import template.crud.dto.TDetailsDTO;
import template.crud.dto.TListItemDTO;
import template.crud.dto.TQueryDTO;

@Replica(name = @Name(regex = "T(.*)$", replacement = "io.cruder.example.generated.user.User$1"), //
		typeRefs = {
				@TypeRef(target = TEntity.class, withType = User.class),
				@TypeRef(target = TAddDTO.class, withType = UserAddDTO.class),
				@TypeRef(target = TDetailsDTO.class, withType = UserDetailsDTO.class),
				@TypeRef(target = TListItemDTO.class, withType = UserListItemDTO.class),
				@TypeRef(target = TQueryDTO.class, withType = UserQueryDTO.class),
				@TypeRef(target = TConverter.class, withName = "io.cruder.example.generated.user.UserConverter"),
				@TypeRef(target = TRepository.class, withName = "io.cruder.example.generated.user.UserRepository"),
		}, //
		literals = {
				@Literal(regex = "#<path>", replacement = "user"),
				@Literal(regex = "#<title>", replacement = "用户"),
		})
public interface UserReplica {

}
