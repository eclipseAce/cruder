package template;

import io.cruder.apt.Replica;
import io.cruder.apt.Replica.Literal;
import io.cruder.apt.Replica.Name;
import io.cruder.apt.Replica.TypeRef;
import io.cruder.example.domain.Role;
import io.cruder.example.dto.role.RoleAddDTO;
import io.cruder.example.dto.role.RoleDetailsDTO;
import io.cruder.example.dto.role.RoleListItemDTO;
import io.cruder.example.dto.role.RoleQueryDTO;
import template.crud.TConverter;
import template.crud.TEntity;
import template.crud.TRepository;
import template.crud.dto.TAddDTO;
import template.crud.dto.TDetailsDTO;
import template.crud.dto.TListItemDTO;
import template.crud.dto.TQueryDTO;

@Replica(name = @Name(regex = "T(.*)$", replacement = "io.cruder.example.generated.role.Role$1"), //
		typeRefs = {
				@TypeRef(replace = TEntity.class, withType = Role.class),
				@TypeRef(replace = TAddDTO.class, withType = RoleAddDTO.class),
				@TypeRef(replace = TDetailsDTO.class, withType = RoleDetailsDTO.class),
				@TypeRef(replace = TListItemDTO.class, withType = RoleListItemDTO.class),
				@TypeRef(replace = TQueryDTO.class, withType = RoleQueryDTO.class),
				@TypeRef(replace = TConverter.class, withName = "io.cruder.example.generated.role.RoleConverter"),
				@TypeRef(replace = TRepository.class, withName = "io.cruder.example.generated.role.RoleRepository"),
		}, //
		literals = {
				@Literal(regex = "#<path>", replacement = "role"),
				@Literal(regex = "#<title>", replacement = "角色"),
		})
public interface RoleReplica {

}
